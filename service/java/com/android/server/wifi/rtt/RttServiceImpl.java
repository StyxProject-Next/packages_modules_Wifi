/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.wifi.rtt;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.rtt.IRttCallback;
import android.net.wifi.rtt.IWifiRttManager;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import com.android.server.wifi.util.NativeUtil;
import com.android.server.wifi.util.WifiPermissionsUtil;

import libcore.util.HexEncoding;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * TBD
 */
public class RttServiceImpl extends IWifiRttManager.Stub {
    private static final String TAG = "RttServiceImpl";
    private static final boolean VDBG = true; // STOPSHIP if true

    private final Context mContext;
    private WifiPermissionsUtil mWifiPermissionsUtil;

    private RttServiceSynchronized mRttServiceSynchronized;


    public RttServiceImpl(Context context) {
        mContext = context;
    }

    /*
     * INITIALIZATION
     */

    /**
     * Initializes the RTT service (usually with objects from an injector).
     *
     * @param looper The looper on which to synchronize operations.
     * @param rttNative The Native interface to the HAL.
     * @param wifiPermissionsUtil Utility for permission checks.
     */
    public void start(Looper looper, RttNative rttNative, WifiPermissionsUtil wifiPermissionsUtil) {
        mWifiPermissionsUtil = wifiPermissionsUtil;
        mRttServiceSynchronized = new RttServiceSynchronized(looper, rttNative);
    }

    /*
     * ASYNCHRONOUS DOMAIN - can be called from different threads!
     */

    /**
     * Proxy for the final native call of the parent class. Enables mocking of
     * the function.
     */
    public int getMockableCallingUid() {
        return getCallingUid();
    }

    /**
     * Binder interface API to start a ranging operation. Called on binder thread, operations needs
     * to be posted to handler thread.
     */
    @Override
    public void startRanging(IBinder binder, String callingPackage, RangingRequest request,
            IRttCallback callback) throws RemoteException {
        if (VDBG) {
            Log.v(TAG, "startRanging: binder=" + binder + ", callingPackage=" + callingPackage
                    + ", request=" + request + ", callback=" + callback);
        }
        // verify arguments
        if (binder == null) {
            throw new IllegalArgumentException("Binder must not be null");
        }
        if (request == null || request.mRttPeers == null || request.mRttPeers.size() == 0) {
            throw new IllegalArgumentException("Request must not be null or empty");
        }
        for (RangingRequest.RttPeer peer: request.mRttPeers) {
            if (peer == null) {
                throw new IllegalArgumentException(
                        "Request must not contain empty peer specifications");
            }
            if (!(peer instanceof RangingRequest.RttPeerAp)) {
                throw new IllegalArgumentException(
                        "Request contains unknown peer specification types");
            }
        }
        if (callback == null) {
            throw new IllegalArgumentException("Callback must not be null");
        }
        request.enforceValidity();

        final int uid = getMockableCallingUid();

        // permission check
        enforceAccessPermission();
        enforceChangePermission();
        enforceLocationPermission(callingPackage, uid);

        // register for binder death
        IBinder.DeathRecipient dr = new IBinder.DeathRecipient() {
            @Override
            public void binderDied() {
                if (VDBG) Log.v(TAG, "binderDied: uid=" + uid);
                binder.unlinkToDeath(this, 0);

                mRttServiceSynchronized.mHandler.post(() -> {
                    mRttServiceSynchronized.cleanUpOnClientDeath(uid);
                });
            }
        };

        try {
            binder.linkToDeath(dr, 0);
        } catch (RemoteException e) {
            Log.e(TAG, "Error on linkToDeath - " + e);
        }

        mRttServiceSynchronized.mHandler.post(() -> {
            mRttServiceSynchronized.startRanging(uid, binder, dr, callingPackage, request,
                    callback);
        });
    }

    /**
     * Called by HAL to report ranging results. Called on HAL thread - needs to post to local
     * thread.
     */
    public void onRangingResults(int cmdId, List<RangingResult> results) {
        if (VDBG) Log.v(TAG, "onRangingResults: cmdId=" + cmdId);
        mRttServiceSynchronized.mHandler.post(() -> {
            mRttServiceSynchronized.onRangingResults(cmdId, results);
        });
    }

    private void enforceAccessPermission() {
        mContext.enforceCallingOrSelfPermission(android.Manifest.permission.ACCESS_WIFI_STATE, TAG);
    }

    private void enforceChangePermission() {
        mContext.enforceCallingOrSelfPermission(android.Manifest.permission.CHANGE_WIFI_STATE, TAG);
    }

    private void enforceLocationPermission(String callingPackage, int uid) {
        mContext.enforceCallingOrSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION,
                TAG);
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (mContext.checkCallingOrSelfPermission(
                android.Manifest.permission.DUMP) != PackageManager.PERMISSION_GRANTED) {
            pw.println("Permission Denial: can't dump RttService from pid="
                    + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        pw.println("Wi-Fi RTT Service");
        mRttServiceSynchronized.dump(fd, pw, args);
    }

    /*
     * SYNCHRONIZED DOMAIN
     */

    /**
     * RTT service implementation - synchronized on a single thread. All commands should be posted
     * to the exposed handler.
     */
    private class RttServiceSynchronized {
        public Handler mHandler;

        private RttNative mRttNative;
        private int mNextCommandId = 1000;
        private RttRequestInfo mCurrentRttRequest = null;

        RttServiceSynchronized(Looper looper, RttNative rttNative) {
            mRttNative = rttNative;

            mHandler = new Handler(looper);
        }

        private void cleanUpOnClientDeath(int uid) {
            Log.d(TAG, "RttServiceSynchronized.cleanUpOnClientDeath: uid=" + uid
                    + ", mCurrentRttRequest=" + mCurrentRttRequest);
            if (mCurrentRttRequest != null && mCurrentRttRequest.uid == uid) {
                // TODO: actually abort operation
                mCurrentRttRequest = null;
            }
        }

        private void startRanging(int uid, IBinder binder, IBinder.DeathRecipient dr,
                String callingPackage, RangingRequest request, IRttCallback callback) {
            mCurrentRttRequest = new RttRequestInfo();
            mCurrentRttRequest.uid = uid;
            mCurrentRttRequest.binder = binder;
            mCurrentRttRequest.dr = dr;
            mCurrentRttRequest.callingPackage = callingPackage;
            mCurrentRttRequest.request = request;
            mCurrentRttRequest.callback = callback;
            mCurrentRttRequest.cmdId = mNextCommandId++;

            if (VDBG) {
                Log.v(TAG, "RttServiceSynchronized.startRanging: mCurrentRttRequest="
                        + mCurrentRttRequest);
            }

            if (!mRttNative.rangeRequest(mCurrentRttRequest.cmdId, request)) {
                mCurrentRttRequest = null;
                Log.w(TAG, "RttServiceSynchronized.startRanging: native rangeRequest call failed");
                try {
                    callback.onRangingResults(RangingResultCallback.STATUS_FAIL, null);
                } catch (RemoteException e) {
                    Log.e(TAG, "RttServiceSynchronized.startRanging: HAL request failed, callback "
                            + "failed -- " + e);
                }
            }
        }

        private void onRangingResults(int cmdId, List<RangingResult> results) {
            if (VDBG) {
                Log.v(TAG, "RttServiceSynchronized.onRangingResults: cmdId=" + cmdId
                        + ", mCurrentRttRequest=" + mCurrentRttRequest + ", results="
                        + Arrays.toString(results.toArray()));
            }
            if (mCurrentRttRequest == null) {
                Log.e(TAG, "RttServiceSynchronized.onRangingResults: no current RTT request "
                        + "pending!?");
                return;
            }

            if (mCurrentRttRequest.cmdId != cmdId) {
                Log.e(TAG, "RttServiceSynchronized.onRangingResults: cmdId=" + cmdId
                        + ", does not match pending RTT request cmdId=" + mCurrentRttRequest.cmdId);
                return;
            }

            boolean permissionGranted = mWifiPermissionsUtil.checkCallersLocationPermission(
                    mCurrentRttRequest.callingPackage, mCurrentRttRequest.uid);
            try {
                if (permissionGranted) {
                    addMissingEntries(mCurrentRttRequest.request, results);
                    mCurrentRttRequest.callback.onRangingResults(
                            RangingResultCallback.STATUS_SUCCESS, results);
                } else {
                    Log.w(TAG, "RttServiceSynchronized.onRangingResults: location permission "
                            + "revoked - not forwarding results");
                    mCurrentRttRequest.callback.onRangingResults(RangingResultCallback.STATUS_FAIL,
                            null);
                }
            } catch (RemoteException e) {
                Log.e(TAG,
                        "RttServiceSynchronized.onRangingResults: callback exception -- " + e);
            }

            // clean-up binder death listener: the callback for results is a onetime event - now
            // done with the binder.
            mCurrentRttRequest.binder.unlinkToDeath(mCurrentRttRequest.dr, 0);

            mCurrentRttRequest = null;
        }

        /*
         * Make sure the results contain an entry for each request. Add results with FAIL status
         * if missing.
         */
        private void addMissingEntries(RangingRequest request,
                List<RangingResult> results) {
            Set<String> resultEntries = new HashSet<>(results.size());
            for (RangingResult result: results) {
                resultEntries.add(new String(HexEncoding.encode(result.getMacAddress())));
            }

            for (RangingRequest.RttPeer peer: request.mRttPeers) {
                byte[] addr;
                if (peer instanceof RangingRequest.RttPeerAp) {
                    addr = NativeUtil.macAddressToByteArray(
                            ((RangingRequest.RttPeerAp) peer).scanResult.BSSID);
                } else {
                    continue;
                }
                String canonicString = new String(HexEncoding.encode(addr));

                if (!resultEntries.contains(canonicString)) {
                    if (VDBG) {
                        Log.v(TAG, "padRangingResultsWithMissingResults: missing=" + canonicString);
                    }
                    results.add(new RangingResult(RangingResultCallback.STATUS_FAIL, addr, 0, 0, 0,
                            0));
                }
            }
        }

        // dump call (asynchronous most likely)
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            pw.println("  mNextCommandId: " + mNextCommandId);
            pw.println("  mCurrentRttRequest: " + mCurrentRttRequest);
            mRttNative.dump(fd, pw, args);
        }
    }

    private static class RttRequestInfo {
        public int uid;
        public IBinder binder;
        public IBinder.DeathRecipient dr;
        public String callingPackage;
        public RangingRequest request;
        public byte[] mac;
        public IRttCallback callback;

        public int cmdId;

        @Override
        public String toString() {
            return new StringBuilder("RttRequestInfo: uid=").append(uid).append(", binder=").append(
                    binder).append(", dr=").append(dr).append(", callingPackage=").append(
                    callingPackage).append(", request=").append(request.toString()).append(
                    ", callback=").append(callback).append(", cmdId=").append(cmdId).toString();
        }
    }
}
