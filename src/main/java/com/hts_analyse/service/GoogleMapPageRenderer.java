package com.hts_analyse.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.hts_analyse.model.dto.GroupedResult;
import java.util.List;

public final class GoogleMapPageRenderer {

    private GoogleMapPageRenderer() {}

    public static String render(List<GroupedResult> data, String googleMapsApiKey) {
        try {
            ObjectMapper om = new ObjectMapper();
            om.registerModule(new JavaTimeModule());
            String json = om.writeValueAsString(data);

            json = json.replace("</", "<\\/");

            return """
                <!doctype html>
                <html lang="tr">
                <head>
                  <meta charset="utf-8" />
                  <title>HTS Eşleşme Haritası</title>
                  <meta name="viewport" content="width=device-width, initial-scale=1" />
                  <style>
                    html,body,#map { height:100%; margin:0; }
                    .panel {
                      position:absolute; top:10px; left:10px; z-index:2;
                      background:rgba(255,255,255,.95); border-radius:10px; padding:10px 12px;
                      box-shadow:0 8px 24px rgba(0,0,0,.12); font-family:system-ui,-apple-system,Segoe UI,Roboto;
                      max-width:380px;
                    }
                    .legend { display:flex; gap:12px; align-items:center; flex-wrap:wrap; }
                    .dot { width:12px; height:12px; border-radius:999px; display:inline-block; }
                    .dot.base  { background:#1e88e5; }
                    .dot.other { background:#e53935; }
                    .controls { margin-top:8px; }
                    .controls button { padding:6px 10px; border:1px solid #ddd; border-radius:8px; background:#fff; cursor:pointer; }
                    .controls button:hover { background:#f4f4f4; }
                  </style>
                </head>
                <body>
                  <div class="panel">
                    <div class="legend">
                      <span class="dot base"></span> <b>Base GSM</b>
                      <span class="dot other"></span> <b>Other GSM</b>
                    </div>
                    <div class="controls">
                      <button id="fitBtn">Tüm noktaları göster</button>
                      <button id="linesBtn">Çizgileri aç/kapat</button>
                    </div>
                    <div style="margin-top:8px;font-size:12px;color:#444">Marker'a tıkla/hover → saatler & istasyonlar.</div>
                  </div>
                
                  <div id="map"></div>
                
                  <script>
                    // Backend'ten gömülen veri:
                    const DATA = %s;
                
                    function icon(color) {
                      return {
                        path: "M12 2C8.14 2 5 5.14 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.86-3.14-7-7-7z",
                        fillColor: color, fillOpacity: 1, strokeColor: "#ffffff", strokeWeight: 2, scale: 1.2,
                        anchor: new google.maps.Point(12, 22)
                      };
                    }
                
                    let map, polylines = [];
                    function initMap() {
                      map = new google.maps.Map(document.getElementById("map"), {
                        center: { lat: 41.01, lng: 29.05 }, zoom: 12, mapTypeControl:false, streetViewControl:false
                      });
                
                      const bounds = new google.maps.LatLngBounds();
                      const iw = new google.maps.InfoWindow();
                
                      (DATA || []).forEach(item => {
                        const basePos  = { lat: item.baseLatitude,  lng: item.baseLongitude };
                        const otherPos = { lat: item.otherLatitude, lng: item.otherLongitude };
                
                        const baseMarker = new google.maps.Marker({ position: basePos,  map, icon: icon("#1e88e5"), title: `[BASE] ${item.baseGsmNumber}` });
                        const otherMarker= new google.maps.Marker({ position: otherPos, map, icon: icon("#e53935"), title: `[OTHER] ${item.otherGsmNumber}` });
                
                        const byDayHtml = (item.byDay || []).map(d =>
                          `<div style="margin-top:6px">
                            <div><b>${d.date}</b> • <span style="color:#666">${d.count} eşleşme</span></div>
                            <div style="margin-top:4px;font-family:monospace;font-size:12px;line-height:1.3">
                              ${(d.pairGroups||[]).map(p => `• ${p.baseTime} ↔ ${p.otherTime}`).join("<br/>")}
                            </div>
                          </div>`).join("");
                
                        const baseHtml = `
                          <div style="min-width:280px">
                            <div style="font-weight:700;color:#1e88e5">BASE • ${item.baseGsmNumber}</div>
                            <div style="margin:4px 0 6px 0">${escapeHtml(item.baseAddress || "")}</div>
                            <div><b>Station IDs:</b> ${(item.baseStationIds||[]).join(", ") || "-"}</div>
                            <div><b>Lat/Lon:</b> ${item.baseLatitude?.toFixed?.(6)}, ${item.baseLongitude?.toFixed?.(6)}</div>
                            <div><b>Diğerine mesafe:</b> ${Math.round(item.distanceMeters)} m</div>
                            ${byDayHtml}
                          </div>`;
                
                        const otherHtml = `
                          <div style="min-width:280px">
                            <div style="font-weight:700;color:#e53935">OTHER • ${item.otherGsmNumber}</div>
                            <div style="margin:4px 0 6px 0">${escapeHtml(item.otherAddress || "")}</div>
                            <div><b>Station IDs:</b> ${(item.otherStationIds||[]).join(", ") || "-"}</div>
                            <div><b>Lat/Lon:</b> ${item.otherLatitude?.toFixed?.(6)}, ${item.otherLongitude?.toFixed?.(6)}</div>
                            <div><b>Base’e mesafe:</b> ${Math.round(item.distanceMeters)} m</div>
                            ${byDayHtml}
                          </div>`;
                
                        baseMarker.addListener("mouseover", () => { iw.setContent(baseHtml); iw.open(map, baseMarker); });
                        baseMarker.addListener("click",     () => { iw.setContent(baseHtml); iw.open(map, baseMarker); });
                        otherMarker.addListener("mouseover", () => { iw.setContent(otherHtml); iw.open(map, otherMarker); });
                        otherMarker.addListener("click",     () => { iw.setContent(otherHtml); iw.open(map, otherMarker); });
                
                        bounds.extend(basePos); bounds.extend(otherPos);
                
                        const line = new google.maps.Polyline({ path:[basePos, otherPos], geodesic:true, strokeOpacity:0.8, strokeWeight:3 });
                        line.setMap(map);
                        polylines.push(line);
                      });
                
                      map.fitBounds(bounds);
                      document.getElementById("fitBtn").onclick   = () => map.fitBounds(bounds);
                      document.getElementById("linesBtn").onclick = () => {
                        const visible = polylines[0]?.getMap() != null;
                        polylines.forEach(pl => pl.setMap(visible ? null : map));
                      };
                    }
                
                    function escapeHtml(s){ return String(s).replace(/[&<>\"']/g, m => ({'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;',\"'\":'&#39;'}[m])); }
                  </script>
                  <script async defer src="https://maps.googleapis.com/maps/api/js?key=%s&callback=initMap"></script>
                </body>
                </html>
                """.formatted(json, googleMapsApiKey == null ? "" : googleMapsApiKey);

        } catch (Exception e) {
            throw new RuntimeException("Map page render failed", e);
        }
    }
}
