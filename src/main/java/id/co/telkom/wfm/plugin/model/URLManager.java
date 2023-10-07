/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.model;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author ASUS
 */
public class URLManager {

    private Map<String, String> urlMap = new HashMap<>();

    // Konstruktor jika Anda ingin menginisialisasi URL awal
    public URLManager() {
        // Contoh inisialisasi URL
        urlMap.put("UIM", "http://10.60.170.43:7051/EnterpriseFeasibilityUim/EnterpriseFeasibilityUimHTTP");
        urlMap.put("ME", "https://api-emas.telkom.co.id:8443/api/device/linkedPort?");
        urlMap.put("MEByIp", "https://api-emas.telkom.co.id:8443/api/device/find?");
        urlMap.put("PEName", "https://api-emas.telkom.co.id:8443/api/device/byServiceArea?");
        urlMap.put("UplinkPort", "https://api-emas.telkom.co.id:8443/api/device/ports?");
        urlMap.put("IMON", "http://eaiesbretail.telkom.co.id:9121/ws/telkom.sb.imon.ws:apiDeployer/telkom_sb_imon_ws_apiDeployer_Port");
        urlMap.put("VLANReservationVCID", "https://api-emas.telkom.co.id:8443/api/vlan/reservationWithVCID");
        urlMap.put("VLANReservation", "https://api-emas.telkom.co.id:8443/api/vlan/reservation?");
        urlMap.put("VRF", "https://api-emas.telkom.co.id:8443/api/vrf/generate");
        urlMap.put("AssociateVRF", "https://api-emas.telkom.co.id:8443/api/vrf/associateToDevice");
        urlMap.put("VRFName", "https://api-emas.telkom.co.id:8443/api/vrf/find?");
        urlMap.put("ValidateSTO", "https://api-emas.telkom.co.id:8443/api/area/stoByCoordinate?");
        urlMap.put("ValidateVRF", "https://api-emas.telkom.co.id:8443/api/vrf/find?");
    }

    // Fungsi untuk menambahkan URL baru
    public void addURL(String key, String url) {
        urlMap.put(key, url);
    }

    // Fungsi untuk mendapatkan URL berdasarkan kunci
    public String getURL(String key) {
        return urlMap.get(key);
    }

    // Fungsi untuk menghapus URL berdasarkan kunci
    public void removeURL(String key) {
        urlMap.remove(key);
    }

}
