/*
 * Copyright (C) 2018 The Android Open Source Project
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

package android.net.wifi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.net.NetworkCapabilities;
import android.os.Parcel;

import androidx.test.filters.SmallTest;

import com.android.modules.utils.build.SdkLevel;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

/**
 * Unit tests for {@link android.net.wifi.WifiInfo}.
 */
@SmallTest
public class WifiInfoTest {
    private static final long TEST_TX_SUCCESS = 1;
    private static final long TEST_TX_RETRIES = 2;
    private static final long TEST_TX_BAD = 3;
    private static final long TEST_RX_SUCCESS = 4;
    private static final String TEST_PACKAGE_NAME = "com.test.example";
    private static final String TEST_FQDN = "test.com";
    private static final String TEST_PROVIDER_NAME = "test";
    private static final int TEST_WIFI_STANDARD = ScanResult.WIFI_STANDARD_11AC;
    private static final int TEST_MAX_SUPPORTED_TX_LINK_SPEED_MBPS = 866;
    private static final int TEST_MAX_SUPPORTED_RX_LINK_SPEED_MBPS = 1200;
    private static final String TEST_SSID = "Test123";
    private static final String TEST_BSSID = "12:12:12:12:12:12";
    private static final int TEST_RSSI = -60;
    private static final int TEST_NETWORK_ID = 5;
    private static final int TEST_NETWORK_ID2 = 6;

    /**
     *  Verify parcel write/read with WifiInfo.
     */
    @Test
    public void testWifiInfoParcelWriteReadWithNoRedactions() throws Exception {
        WifiInfo writeWifiInfo = new WifiInfo();
        writeWifiInfo.txSuccess = TEST_TX_SUCCESS;
        writeWifiInfo.txRetries = TEST_TX_RETRIES;
        writeWifiInfo.txBad = TEST_TX_BAD;
        writeWifiInfo.rxSuccess = TEST_RX_SUCCESS;
        writeWifiInfo.setSSID(WifiSsid.createFromAsciiEncoded(TEST_SSID));
        writeWifiInfo.setBSSID(TEST_BSSID);
        writeWifiInfo.setNetworkId(TEST_NETWORK_ID);
        writeWifiInfo.setTrusted(true);
        writeWifiInfo.setOsuAp(true);
        writeWifiInfo.setFQDN(TEST_FQDN);
        writeWifiInfo.setProviderFriendlyName(TEST_PROVIDER_NAME);
        writeWifiInfo.setRequestingPackageName(TEST_PACKAGE_NAME);
        writeWifiInfo.setWifiStandard(TEST_WIFI_STANDARD);
        writeWifiInfo.setMaxSupportedTxLinkSpeedMbps(TEST_MAX_SUPPORTED_TX_LINK_SPEED_MBPS);
        writeWifiInfo.setMaxSupportedRxLinkSpeedMbps(TEST_MAX_SUPPORTED_RX_LINK_SPEED_MBPS);
        writeWifiInfo.setMacAddress(TEST_BSSID);

        // Make a copy which allows parcelling of location sensitive data.
        WifiInfo writeWifiInfoCopy = writeWifiInfo.makeCopy(NetworkCapabilities.REDACT_NONE);

        Parcel parcel = Parcel.obtain();
        writeWifiInfoCopy.writeToParcel(parcel, 0);
        // Rewind the pointer to the head of the parcel.
        parcel.setDataPosition(0);
        WifiInfo readWifiInfo = WifiInfo.CREATOR.createFromParcel(parcel);

        assertNotNull(readWifiInfo);
        assertEquals(TEST_TX_SUCCESS, readWifiInfo.txSuccess);
        assertEquals(TEST_TX_RETRIES, readWifiInfo.txRetries);
        assertEquals(TEST_TX_BAD, readWifiInfo.txBad);
        assertEquals(TEST_RX_SUCCESS, readWifiInfo.rxSuccess);
        assertEquals("\"" + TEST_SSID + "\"", readWifiInfo.getSSID());
        assertEquals(TEST_BSSID, readWifiInfo.getBSSID());
        assertEquals(TEST_NETWORK_ID, readWifiInfo.getNetworkId());
        assertTrue(readWifiInfo.isTrusted());
        assertTrue(readWifiInfo.isOsuAp());
        assertTrue(readWifiInfo.isPasspointAp());
        assertEquals(TEST_PACKAGE_NAME, readWifiInfo.getRequestingPackageName());
        assertEquals(TEST_FQDN, readWifiInfo.getPasspointFqdn());
        assertEquals(TEST_PROVIDER_NAME, readWifiInfo.getPasspointProviderFriendlyName());
        assertEquals(TEST_WIFI_STANDARD, readWifiInfo.getWifiStandard());
        assertEquals(TEST_MAX_SUPPORTED_TX_LINK_SPEED_MBPS,
                readWifiInfo.getMaxSupportedTxLinkSpeedMbps());
        assertEquals(TEST_MAX_SUPPORTED_RX_LINK_SPEED_MBPS,
                readWifiInfo.getMaxSupportedRxLinkSpeedMbps());
        assertEquals(TEST_BSSID, readWifiInfo.getMacAddress());
    }

    /**
     *  Verify parcel write/read with WifiInfo.
     */
    @Test
    public void testWifiInfoParcelWriteReadWithoutLocationSensitiveInfo() throws Exception {
        WifiInfo writeWifiInfo = new WifiInfo();
        writeWifiInfo.txSuccess = TEST_TX_SUCCESS;
        writeWifiInfo.txRetries = TEST_TX_RETRIES;
        writeWifiInfo.txBad = TEST_TX_BAD;
        writeWifiInfo.rxSuccess = TEST_RX_SUCCESS;
        writeWifiInfo.setSSID(WifiSsid.createFromAsciiEncoded(TEST_SSID));
        writeWifiInfo.setBSSID(TEST_BSSID);
        writeWifiInfo.setNetworkId(TEST_NETWORK_ID);
        writeWifiInfo.setTrusted(true);
        writeWifiInfo.setOsuAp(true);
        writeWifiInfo.setFQDN(TEST_FQDN);
        writeWifiInfo.setProviderFriendlyName(TEST_PROVIDER_NAME);
        writeWifiInfo.setRequestingPackageName(TEST_PACKAGE_NAME);
        writeWifiInfo.setWifiStandard(TEST_WIFI_STANDARD);
        writeWifiInfo.setMaxSupportedTxLinkSpeedMbps(TEST_MAX_SUPPORTED_TX_LINK_SPEED_MBPS);
        writeWifiInfo.setMaxSupportedRxLinkSpeedMbps(TEST_MAX_SUPPORTED_RX_LINK_SPEED_MBPS);
        writeWifiInfo.setMacAddress(TEST_BSSID);

        WifiInfo writeWifiInfoCopy =
                writeWifiInfo.makeCopy(NetworkCapabilities.REDACT_FOR_ACCESS_FINE_LOCATION);

        Parcel parcel = Parcel.obtain();
        writeWifiInfoCopy.writeToParcel(parcel, 0);
        // Rewind the pointer to the head of the parcel.
        parcel.setDataPosition(0);
        WifiInfo readWifiInfo = WifiInfo.CREATOR.createFromParcel(parcel);

        assertNotNull(readWifiInfo);
        assertEquals(TEST_TX_SUCCESS, readWifiInfo.txSuccess);
        assertEquals(TEST_TX_RETRIES, readWifiInfo.txRetries);
        assertEquals(TEST_TX_BAD, readWifiInfo.txBad);
        assertEquals(TEST_RX_SUCCESS, readWifiInfo.rxSuccess);
        assertEquals(WifiManager.UNKNOWN_SSID, readWifiInfo.getSSID());
        assertEquals(WifiInfo.DEFAULT_MAC_ADDRESS, readWifiInfo.getBSSID());
        assertEquals(WifiConfiguration.INVALID_NETWORK_ID, readWifiInfo.getNetworkId());
        assertTrue(readWifiInfo.isTrusted());
        assertTrue(readWifiInfo.isOsuAp());
        assertFalse(readWifiInfo.isPasspointAp()); // fqdn & friendly name is masked.
        assertEquals(TEST_PACKAGE_NAME, readWifiInfo.getRequestingPackageName());
        assertNull(readWifiInfo.getPasspointFqdn());
        assertNull(readWifiInfo.getPasspointProviderFriendlyName());
        assertEquals(TEST_WIFI_STANDARD, readWifiInfo.getWifiStandard());
        assertEquals(TEST_MAX_SUPPORTED_TX_LINK_SPEED_MBPS,
                readWifiInfo.getMaxSupportedTxLinkSpeedMbps());
        assertEquals(TEST_MAX_SUPPORTED_RX_LINK_SPEED_MBPS,
                readWifiInfo.getMaxSupportedRxLinkSpeedMbps());
        assertEquals(WifiInfo.DEFAULT_MAC_ADDRESS, readWifiInfo.getMacAddress());
    }

    /**
     *  Verify parcel write/read with WifiInfo.
     */
    @Test
    public void testWifiInfoParcelWriteReadWithoutLocalMacAddressInfo() throws Exception {
        WifiInfo writeWifiInfo = new WifiInfo();
        writeWifiInfo.setMacAddress(TEST_BSSID);

        WifiInfo writeWifiInfoCopy =
                writeWifiInfo.makeCopy(NetworkCapabilities.REDACT_FOR_LOCAL_MAC_ADDRESS);

        Parcel parcel = Parcel.obtain();
        writeWifiInfoCopy.writeToParcel(parcel, 0);
        // Rewind the pointer to the head of the parcel.
        parcel.setDataPosition(0);
        WifiInfo readWifiInfo = WifiInfo.CREATOR.createFromParcel(parcel);

        assertNotNull(readWifiInfo);
        assertEquals(WifiInfo.DEFAULT_MAC_ADDRESS, readWifiInfo.getMacAddress());
    }

    @Test
    public void testWifiInfoGetApplicableRedactions() throws Exception {
        long redactions = new WifiInfo().getApplicableRedactions();
        assertEquals(NetworkCapabilities.REDACT_FOR_ACCESS_FINE_LOCATION
                | NetworkCapabilities.REDACT_FOR_LOCAL_MAC_ADDRESS
                | NetworkCapabilities.REDACT_FOR_NETWORK_SETTINGS, redactions);
    }

    @Test
    public void testWifiInfoParcelWriteReadWithoutLocationAndLocalMacAddressSensitiveInfo()
            throws Exception {
        assumeTrue(SdkLevel.isAtLeastS());

        WifiInfo writeWifiInfo = new WifiInfo();
        writeWifiInfo.setSSID(WifiSsid.createFromAsciiEncoded(TEST_SSID));
        writeWifiInfo.setBSSID(TEST_BSSID);
        writeWifiInfo.setNetworkId(TEST_NETWORK_ID);
        writeWifiInfo.setFQDN(TEST_FQDN);
        writeWifiInfo.setProviderFriendlyName(TEST_PROVIDER_NAME);
        writeWifiInfo.setMacAddress(TEST_BSSID);

        WifiInfo writeWifiInfoCopy =
                writeWifiInfo.makeCopy(NetworkCapabilities.REDACT_FOR_ACCESS_FINE_LOCATION
                        | NetworkCapabilities.REDACT_FOR_LOCAL_MAC_ADDRESS);

        Parcel parcel = Parcel.obtain();
        writeWifiInfoCopy.writeToParcel(parcel, 0);
        // Rewind the pointer to the head of the parcel.
        parcel.setDataPosition(0);
        WifiInfo readWifiInfo = WifiInfo.CREATOR.createFromParcel(parcel);

        assertNotNull(readWifiInfo);
        assertEquals(WifiManager.UNKNOWN_SSID, readWifiInfo.getSSID());
        assertEquals(WifiInfo.DEFAULT_MAC_ADDRESS, readWifiInfo.getBSSID());
        assertEquals(WifiConfiguration.INVALID_NETWORK_ID, readWifiInfo.getNetworkId());
        assertNull(readWifiInfo.getPasspointFqdn());
        assertNull(readWifiInfo.getPasspointProviderFriendlyName());
        assertEquals(WifiInfo.DEFAULT_MAC_ADDRESS, readWifiInfo.getMacAddress());
    }

    @Test
    public void testWifiInfoCopyConstructor() throws Exception {
        WifiInfo writeWifiInfo = new WifiInfo();
        writeWifiInfo.txSuccess = TEST_TX_SUCCESS;
        writeWifiInfo.txRetries = TEST_TX_RETRIES;
        writeWifiInfo.txBad = TEST_TX_BAD;
        writeWifiInfo.rxSuccess = TEST_RX_SUCCESS;
        writeWifiInfo.setTrusted(true);
        writeWifiInfo.setOsuAp(true);
        writeWifiInfo.setFQDN(TEST_FQDN);
        writeWifiInfo.setProviderFriendlyName(TEST_PROVIDER_NAME);
        writeWifiInfo.setRequestingPackageName(TEST_PACKAGE_NAME);
        writeWifiInfo.setWifiStandard(TEST_WIFI_STANDARD);
        writeWifiInfo.setMaxSupportedTxLinkSpeedMbps(TEST_MAX_SUPPORTED_TX_LINK_SPEED_MBPS);
        writeWifiInfo.setMaxSupportedRxLinkSpeedMbps(TEST_MAX_SUPPORTED_RX_LINK_SPEED_MBPS);

        WifiInfo readWifiInfo = new WifiInfo(writeWifiInfo);

        assertEquals(TEST_TX_SUCCESS, readWifiInfo.txSuccess);
        assertEquals(TEST_TX_RETRIES, readWifiInfo.txRetries);
        assertEquals(TEST_TX_BAD, readWifiInfo.txBad);
        assertEquals(TEST_RX_SUCCESS, readWifiInfo.rxSuccess);
        assertTrue(readWifiInfo.isTrusted());
        assertTrue(readWifiInfo.isOsuAp());
        assertTrue(readWifiInfo.isPasspointAp());
        assertEquals(TEST_PACKAGE_NAME, readWifiInfo.getRequestingPackageName());
        assertEquals(TEST_FQDN, readWifiInfo.getPasspointFqdn());
        assertEquals(TEST_PROVIDER_NAME, readWifiInfo.getPasspointProviderFriendlyName());
        assertEquals(TEST_WIFI_STANDARD, readWifiInfo.getWifiStandard());
        assertEquals(TEST_MAX_SUPPORTED_TX_LINK_SPEED_MBPS,
                readWifiInfo.getMaxSupportedTxLinkSpeedMbps());
        assertEquals(TEST_MAX_SUPPORTED_RX_LINK_SPEED_MBPS,
                readWifiInfo.getMaxSupportedRxLinkSpeedMbps());
    }

    /**
     *  Verify values after reset()
     */
    @Test
    public void testWifiInfoResetValue() throws Exception {
        WifiInfo wifiInfo = new WifiInfo();
        wifiInfo.reset();
        assertEquals(WifiInfo.LINK_SPEED_UNKNOWN, wifiInfo.getMaxSupportedTxLinkSpeedMbps());
        assertEquals(WifiInfo.LINK_SPEED_UNKNOWN, wifiInfo.getMaxSupportedRxLinkSpeedMbps());
        assertEquals(WifiInfo.LINK_SPEED_UNKNOWN, wifiInfo.getTxLinkSpeedMbps());
        assertEquals(WifiInfo.LINK_SPEED_UNKNOWN, wifiInfo.getRxLinkSpeedMbps());
        assertEquals(WifiInfo.INVALID_RSSI, wifiInfo.getRssi());
        assertEquals(WifiManager.UNKNOWN_SSID, wifiInfo.getSSID());
        assertEquals(null, wifiInfo.getBSSID());
        assertEquals(-1, wifiInfo.getNetworkId());
    }

    /**
     * Test that the WifiInfo Builder returns the same values that was set, and that
     * calling build multiple times returns different instances.
     */
    @Test
    public void testWifiInfoBuilder() throws Exception {
        WifiInfo.Builder builder = new WifiInfo.Builder()
                .setSsid(TEST_SSID.getBytes(StandardCharsets.UTF_8))
                .setBssid(TEST_BSSID)
                .setRssi(TEST_RSSI)
                .setNetworkId(TEST_NETWORK_ID);

        WifiInfo info1 = builder.build();

        assertEquals("\"" + TEST_SSID + "\"", info1.getSSID());
        assertEquals(TEST_BSSID, info1.getBSSID());
        assertEquals(TEST_RSSI, info1.getRssi());
        assertEquals(TEST_NETWORK_ID, info1.getNetworkId());

        WifiInfo info2 = builder
                .setNetworkId(TEST_NETWORK_ID2)
                .build();

        // different instances
        assertNotSame(info1, info2);

        // assert that info1 didn't change
        assertEquals("\"" + TEST_SSID + "\"", info1.getSSID());
        assertEquals(TEST_BSSID, info1.getBSSID());
        assertEquals(TEST_RSSI, info1.getRssi());
        assertEquals(TEST_NETWORK_ID, info1.getNetworkId());

        // assert that info2 changed
        assertEquals("\"" + TEST_SSID + "\"", info2.getSSID());
        assertEquals(TEST_BSSID, info2.getBSSID());
        assertEquals(TEST_RSSI, info2.getRssi());
        assertEquals(TEST_NETWORK_ID2, info2.getNetworkId());
    }

    @Test
    public void testWifiInfoEquals() throws Exception {
        WifiInfo.Builder builder = new WifiInfo.Builder()
                .setSsid(TEST_SSID.getBytes(StandardCharsets.UTF_8))
                .setBssid(TEST_BSSID)
                .setRssi(TEST_RSSI)
                .setNetworkId(TEST_NETWORK_ID);

        WifiInfo info1 = builder.build();
        WifiInfo info2 = builder.build();
        if (SdkLevel.isAtLeastS()) {
            assertEquals(info1, info2);
        } else {
            assertNotEquals(info1, info2);
        }

        info1.setTrusted(true);
        // Same behavior pre-S & post-S.
        assertNotEquals(info1, info2);

        info2.setTrusted(true);
        if (SdkLevel.isAtLeastS()) {
            assertEquals(info1, info2);
        } else {
            assertNotEquals(info1, info2);
        }

        info1.setSSID(WifiSsid.createFromHex(null));
        // Same behavior pre-S & post-S.
        assertNotEquals(info1, info2);

        info2.setSSID(WifiSsid.createFromHex(null));
        if (SdkLevel.isAtLeastS()) {
            assertEquals(info1, info2);
        } else {
            assertNotEquals(info1, info2);
        }
    }

    @Test
    public void testWifiInfoHashcode() throws Exception {
        WifiInfo.Builder builder = new WifiInfo.Builder()
                .setSsid(TEST_SSID.getBytes(StandardCharsets.UTF_8))
                .setBssid(TEST_BSSID)
                .setRssi(TEST_RSSI)
                .setNetworkId(TEST_NETWORK_ID);

        WifiInfo info1 = builder.build();
        WifiInfo info2 = builder.build();
        if (SdkLevel.isAtLeastS()) {
            assertEquals(info1.hashCode(), info2.hashCode());
        } else {
            assertNotEquals(info1.hashCode(), info2.hashCode());
        }

        info1.setTrusted(true);
        // Same behavior pre-S & post-S.
        assertNotEquals(info1.hashCode(), info2.hashCode());

        info2.setTrusted(true);
        if (SdkLevel.isAtLeastS()) {
            assertEquals(info1.hashCode(), info2.hashCode());
        } else {
            assertNotEquals(info1.hashCode(), info2.hashCode());
        }

        info1.setSSID(WifiSsid.createFromHex(null));
        // Same behavior pre-S & post-S.
        assertNotEquals(info1.hashCode(), info2.hashCode());

        info2.setSSID(WifiSsid.createFromHex(null));
        if (SdkLevel.isAtLeastS()) {
            assertEquals(info1.hashCode(), info2.hashCode());
        } else {
            assertNotEquals(info1.hashCode(), info2.hashCode());
        }
    }
}
