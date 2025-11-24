```python
import pandas as pd
import trino
import json

# Cấu hình để Pandas hiển thị đầy đủ nội dung (quan trọng khi xem JSON)
pd.set_option('display.max_rows', 500)
pd.set_option('display.max_columns', 500)
pd.set_option('display.max_colwidth', None)

print("Các thư viện đã sẵn sàng.")
```

    Các thư viện đã sẵn sàng.
    


```python
# Kết nối tới Trino service
conn = trino.dbapi.connect(
    host="127.0.0.1",
    port=8083,
    user="don",
    catalog="lakehouse",   # hoặc "iceberg" tùy SHOW CATALOGS
    schema="rva",       # đây là database trong iceberg
)


cursor = conn.cursor()
print(f"Kết nối Trino (Catalog: {conn.catalog}, Schema: {conn.schema}) thành công!")
```

    Kết nối Trino (Catalog: lakehouse, Schema: rva) thành công!
    


```python
cursor.execute("SHOW TABLES")
tables = cursor.fetchall()
print("Các bảng có trong schema 'retail':")
print(tables)

# Kết quả mong đợi: [('bronze_raw',)]
```

    Các bảng có trong schema 'retail':
    [['bronze_raw'], ['silver_detections']]
    

#### Đọc dữ liệu Bronze bằng Trino


```python
cursor.execute("SELECT * FROM bronze_raw LIMIT 10")
rows = cursor.fetchall()
print(rows)

```

    [['v1', None, None, '{"schema_version": "1.0", "pipeline_run_id": "7651a7e53b0e4129be373557ab56378a", "source": {"store_id": "store_01", "camera_id": "cam_01", "stream_id": "stream_01"}, "frame_index": 1, "capture_ts": "2025-11-21T19:48:50.907551+00:00", "image_size": {"width": 1280, "height": 720}, "detections": [{"det_id": "1-0", "class": "person", "class_id": 0, "conf": 0.879061222076416, "bbox": {"x1": 547.5609130859375, "y1": 208.7528076171875, "x2": 647.697265625, "y2": 566.1458740234375}, "bbox_norm": {"x": 0.4277819633483887, "y": 0.28993445502387155, "w": 0.07823152542114258, "h": 0.49637925889756945}, "centroid": {"x": 597, "y": 387}, "centroid_norm": {"x": 0.46689772605895996, "y": 0.5381240844726562}, "track_id": 1}, {"det_id": "1-1", "class": "person", "class_id": 0, "conf": 0.8788681030273438, "bbox": {"x1": 720.4228515625, "y1": 106.02847290039062, "x2": 808.581298828125, "y2": 287.6534729003906}, "bbox_norm": {"x": 0.5628303527832031, "y": 0.1472617679172092, "w": 0.06887378692626953, "h": 0.25225694444444446}, "centroid": {"x": 764, "y": 196}, "centroid_norm": {"x": 0.5972672462463379, "y": 0.2733902401394314}, "track_id": 2}, {"det_id": "1-2", "class": "person", "class_id": 0, "conf": 0.8445228338241577, "bbox": {"x1": 0.01546478271484375, "y1": 223.85882568359375, "x2": 66.65916442871094, "y2": 503.38714599609375}, "bbox_norm": {"x": 1.2081861495971679e-05, "y": 0.310915035671658, "w": 0.05206539034843445, "h": 0.38823377821180555}, "centroid": {"x": 33, "y": 363}, "centroid_norm": {"x": 0.026044777035713194, "y": 0.5050319247775608}, "track_id": 3}, {"det_id": "1-3", "class": "person", "class_id": 0, "conf": 0.8155996799468994, "bbox": {"x1": 926.5863037109375, "y1": 0.5677490234375, "x2": 1012.654052734375, "y2": 214.39083862304688}, "bbox_norm": {"x": 0.7238955497741699, "y": 0.0007885403103298611, "w": 0.06724042892456054, "h": 0.2969765133327908}, "centroid": {"x": 969, "y": 107}, "centroid_norm": {"x": 0.7575157642364502, "y": 0.14927679697672527}, "track_id": 4}], "runtime": {"model_name": "yolo11l.pt", "tracker_type": "botsort", "conf_thres": 0.25, "class_filter": [0]}, "source_uri": "D:\\\\DockerData\\\\retail-video-analytics\\\\vision\\\\video\\\\video3.mp4"}', 'cam_01', 'store_01', datetime.datetime(2025, 11, 22, 13, 10, 5, 546000)], ['v1', None, None, '{"schema_version": "1.0", "pipeline_run_id": "7651a7e53b0e4129be373557ab56378a", "source": {"store_id": "store_01", "camera_id": "cam_01", "stream_id": "stream_01"}, "frame_index": 2, "capture_ts": "2025-11-21T19:48:51.165038+00:00", "image_size": {"width": 1280, "height": 720}, "detections": [{"det_id": "2-0", "class": "person", "class_id": 0, "conf": 0.8791031241416931, "bbox": {"x1": 547.560791015625, "y1": 208.7517547607422, "x2": 647.6970825195312, "y2": 566.1426391601562}, "bbox_norm": {"x": 0.427781867980957, "y": 0.289932992723253, "w": 0.07823147773742675, "h": 0.49637622833251954}, "centroid": {"x": 597, "y": 387}, "centroid_norm": {"x": 0.4668976068496704, "y": 0.5381211068895128}, "track_id": 1}, {"det_id": "2-1", "class": "person", "class_id": 0, "conf": 0.8789587020874023, "bbox": {"x1": 720.4228515625, "y1": 106.02108764648438, "x2": 808.5772705078125, "y2": 287.6536560058594}, "bbox_norm": {"x": 0.5628303527832031, "y": 0.14725151062011718, "w": 0.0688706398010254, "h": 0.2522674560546875}, "centroid": {"x": 764, "y": 196}, "centroid_norm": {"x": 0.5972656726837158, "y": 0.27338523864746095}, "track_id": 2}, {"det_id": "2-2", "class": "person", "class_id": 0, "conf": 0.8445611596107483, "bbox": {"x1": 0.0154449213296175, "y1": 223.8511962890625, "x2": 66.65782928466797, "y2": 503.38836669921875}, "bbox_norm": {"x": 1.2066344788763672e-05, "y": 0.3109044392903646, "w": 0.052064362783858086, "h": 0.3882460700141059}, "centroid": {"x": 33, "y": 363}, "centroid_norm": {"x": 0.026044247736717808, "y": 0.5050274742974176}, "track_id": 3}, {"det_id": "2-3", "class": "person", "class_id": 0, "conf": 0.8156672120094299, "bbox": {"x1": 926.5841674804688, "y1": 0.566967785358429, "x2": 1012.6541748046875, "y2": 214.39312744140625}, "bbox_norm": {"x": 0.7238938808441162, "y": 0.0007874552574422624, "w": 0.0672421932220459, "h": 0.29698077730006645}, "centroid": {"x": 969, "y": 107}, "centroid_norm": {"x": 0.7575149774551392, "y": 0.14927784390747548}, "track_id": 4}], "runtime": {"model_name": "yolo11l.pt", "tracker_type": "botsort", "conf_thres": 0.25, "class_filter": [0]}, "source_uri": "D:\\\\DockerData\\\\retail-video-analytics\\\\vision\\\\video\\\\video3.mp4"}', 'cam_01', 'store_01', datetime.datetime(2025, 11, 22, 13, 10, 6, 230000)], ['v1', None, None, '{"schema_version": "1.0", "pipeline_run_id": "7651a7e53b0e4129be373557ab56378a", "source": {"store_id": "store_01", "camera_id": "cam_01", "stream_id": "stream_01"}, "frame_index": 3, "capture_ts": "2025-11-21T19:48:51.270014+00:00", "image_size": {"width": 1280, "height": 720}, "detections": [{"det_id": "3-0", "class": "person", "class_id": 0, "conf": 0.8791031241416931, "bbox": {"x1": 547.560791015625, "y1": 208.75157165527344, "x2": 647.697021484375, "y2": 566.14208984375}, "bbox_norm": {"x": 0.427781867980957, "y": 0.289932738410102, "w": 0.07823143005371094, "h": 0.49637571970621747}, "centroid": {"x": 597, "y": 387}, "centroid_norm": {"x": 0.4668975830078125, "y": 0.5381205982632107}, "track_id": 1}, {"det_id": "3-1", "class": "person", "class_id": 0, "conf": 0.8789587020874023, "bbox": {"x1": 720.4228515625, "y1": 106.01982116699219, "x2": 808.5765991210938, "y2": 287.6536865234375}, "bbox_norm": {"x": 0.5628303527832031, "y": 0.1472497516208225, "w": 0.06887011528015137, "h": 0.2522692574395074}, "centroid": {"x": 764, "y": 196}, "centroid_norm": {"x": 0.5972654104232789, "y": 0.27338438034057616}, "track_id": 2}, {"det_id": "3-2", "class": "person", "class_id": 0, "conf": 0.8445611596107483, "bbox": {"x1": 0.01544151920825243, "y1": 223.8498992919922, "x2": 66.65760040283203, "y2": 503.3885803222656}, "bbox_norm": {"x": 1.2063686881447211e-05, "y": 0.31090263790554473, "w": 0.05206418662783108, "h": 0.388248168097602}, "centroid": {"x": 33, "y": 363}, "centroid_norm": {"x": 0.026044157000796986, "y": 0.5050267219543457}, "track_id": 3}, {"det_id": "3-3", "class": "person", "class_id": 0, "conf": 0.8156672120094299, "bbox": {"x1": 926.5838012695312, "y1": 0.5668339729309082, "x2": 1012.6541748046875, "y2": 214.39352416992188}, "bbox_norm": {"x": 0.7238935947418212, "y": 0.0007872694068484837, "w": 0.06724247932434083, "h": 0.29698151416248747}, "centroid": {"x": 969, "y": 107}, "centroid_norm": {"x": 0.7575148344039917, "y": 0.1492780264880922}, "track_id": 4}], "runtime": {"model_name": "yolo11l.pt", "tracker_type": "botsort", "conf_thres": 0.25, "class_filter": [0]}, "source_uri": "D:\\\\DockerData\\\\retail-video-analytics\\\\vision\\\\video\\\\video3.mp4"}', 'cam_01', 'store_01', datetime.datetime(2025, 11, 22, 13, 10, 6, 233000)], ['v1', None, None, '{"schema_version": "1.0", "pipeline_run_id": "7651a7e53b0e4129be373557ab56378a", "source": {"store_id": "store_01", "camera_id": "cam_01", "stream_id": "stream_01"}, "frame_index": 4, "capture_ts": "2025-11-21T19:48:51.378599+00:00", "image_size": {"width": 1280, "height": 720}, "detections": [{"det_id": "4-0", "class": "person", "class_id": 0, "conf": 0.8791031241416931, "bbox": {"x1": 547.560791015625, "y1": 208.75152587890625, "x2": 647.697021484375, "y2": 566.1419677734375}, "bbox_norm": {"x": 0.427781867980957, "y": 0.28993267483181423, "w": 0.07823143005371094, "h": 0.4963756137424045}, "centroid": {"x": 597, "y": 387}, "centroid_norm": {"x": 0.4668975830078125, "y": 0.5381204817030165}, "track_id": 1}, {"det_id": "4-1", "class": "person", "class_id": 0, "conf": 0.8789587020874023, "bbox": {"x1": 720.4228515625, "y1": 106.0195541381836, "x2": 808.576416015625, "y2": 287.6536865234375}, "bbox_norm": {"x": 0.5628303527832031, "y": 0.1472493807474772, "w": 0.0688699722290039, "h": 0.25226962831285266}, "centroid": {"x": 764, "y": 196}, "centroid_norm": {"x": 0.5972653388977051, "y": 0.27338419490390353}, "track_id": 2}, {"det_id": "4-2", "class": "person", "class_id": 0, "conf": 0.8445611596107483, "bbox": {"x1": 0.015440816059708595, "y1": 223.84962463378906, "x2": 66.65754699707031, "y2": 503.38861083984375}, "bbox_norm": {"x": 1.206313754664734e-05, "y": 0.31090225643581815, "w": 0.05206414545391454, "h": 0.3882485919528537}, "centroid": {"x": 33, "y": 363}, "centroid_norm": {"x": 0.026044135864503916, "y": 0.505026552412245}, "track_id": 3}, {"det_id": "4-3", "class": "person", "class_id": 0, "conf": 0.8156672120094299, "bbox": {"x1": 926.583740234375, "y1": 0.5668063163757324, "x2": 1012.6541748046875, "y2": 214.3936004638672}, "bbox_norm": {"x": 0.7238935470581055, "y": 0.000787230994966295, "w": 0.06724252700805664, "h": 0.2969816585381826}, "centroid": {"x": 969, "y": 107}, "centroid_norm": {"x": 0.7575148105621338, "y": 0.14927806026405757}, "track_id": 4}], "runtime": {"model_name": "yolo11l.pt", "tracker_type": "botsort", "conf_thres": 0.25, "class_filter": [0]}, "source_uri": "D:\\\\DockerData\\\\retail-video-analytics\\\\vision\\\\video\\\\video3.mp4"}', 'cam_01', 'store_01', datetime.datetime(2025, 11, 22, 13, 10, 6, 235000)], ['v1', None, None, '{"schema_version": "1.0", "pipeline_run_id": "7651a7e53b0e4129be373557ab56378a", "source": {"store_id": "store_01", "camera_id": "cam_01", "stream_id": "stream_01"}, "frame_index": 5, "capture_ts": "2025-11-21T19:48:51.473212+00:00", "image_size": {"width": 1280, "height": 720}, "detections": [{"det_id": "5-0", "class": "person", "class_id": 0, "conf": 0.8791031241416931, "bbox": {"x1": 547.560791015625, "y1": 208.75152587890625, "x2": 647.697021484375, "y2": 566.1419677734375}, "bbox_norm": {"x": 0.427781867980957, "y": 0.28993267483181423, "w": 0.07823143005371094, "h": 0.4963756137424045}, "centroid": {"x": 597, "y": 387}, "centroid_norm": {"x": 0.4668975830078125, "y": 0.5381204817030165}, "track_id": 1}, {"det_id": "5-1", "class": "person", "class_id": 0, "conf": 0.8789587020874023, "bbox": {"x1": 720.4228515625, "y1": 106.01956176757812, "x2": 808.576416015625, "y2": 287.6536865234375}, "bbox_norm": {"x": 0.5628303527832031, "y": 0.1472493913438585, "w": 0.0688699722290039, "h": 0.25226961771647133}, "centroid": {"x": 764, "y": 196}, "centroid_norm": {"x": 0.5972653388977051, "y": 0.2733842002020942}, "track_id": 2}, {"det_id": "5-2", "class": "person", "class_id": 0, "conf": 0.8445611596107483, "bbox": {"x1": 0.015440832823514938, "y1": 223.84962463378906, "x2": 66.65755462646484, "y2": 503.38861083984375}, "bbox_norm": {"x": 1.2063150643371046e-05, "y": 0.31090225643581815, "w": 0.05206415140128229, "h": 0.3882485919528537}, "centroid": {"x": 33, "y": 363}, "centroid_norm": {"x": 0.026044138851284517, "y": 0.505026552412245}, "track_id": 3}, {"det_id": "5-3", "class": "person", "class_id": 0, "conf": 0.8156672120094299, "bbox": {"x1": 926.583740234375, "y1": 0.566806972026825, "x2": 1012.6541748046875, "y2": 214.3936004638672}, "bbox_norm": {"x": 0.7238935470581055, "y": 0.0007872319055928124, "w": 0.06724252700805664, "h": 0.2969816576275561}, "centroid": {"x": 969, "y": 107}, "centroid_norm": {"x": 0.7575148105621338, "y": 0.14927806071937083}, "track_id": 4}], "runtime": {"model_name": "yolo11l.pt", "tracker_type": "botsort", "conf_thres": 0.25, "class_filter": [0]}, "source_uri": "D:\\\\DockerData\\\\retail-video-analytics\\\\vision\\\\video\\\\video3.mp4"}', 'cam_01', 'store_01', datetime.datetime(2025, 11, 22, 13, 10, 6, 236000)], ['v1', None, None, '{"schema_version": "1.0", "pipeline_run_id": "7651a7e53b0e4129be373557ab56378a", "source": {"store_id": "store_01", "camera_id": "cam_01", "stream_id": "stream_01"}, "frame_index": 6, "capture_ts": "2025-11-21T19:48:51.575889+00:00", "image_size": {"width": 1280, "height": 720}, "detections": [{"det_id": "6-0", "class": "person", "class_id": 0, "conf": 0.8791399002075195, "bbox": {"x1": 547.56298828125, "y1": 208.75421142578125, "x2": 647.6973266601562, "y2": 566.1451416015625}, "bbox_norm": {"x": 0.42778358459472654, "y": 0.2899364047580295, "w": 0.07822995185852051, "h": 0.4963762919108073}, "centroid": {"x": 597, "y": 387}, "centroid_norm": {"x": 0.4668985605239868, "y": 0.5381245507134331}, "track_id": 1}, {"det_id": "6-1", "class": "person", "class_id": 0, "conf": 0.8789353370666504, "bbox": {"x1": 720.4228515625, "y1": 106.02349090576172, "x2": 808.57666015625, "y2": 287.6535339355469}, "bbox_norm": {"x": 0.5628303527832031, "y": 0.1472548484802246, "w": 0.06887016296386719, "h": 0.25226394865247936}, "centroid": {"x": 764, "y": 196}, "centroid_norm": {"x": 0.5972654342651367, "y": 0.2733868228064643}, "track_id": 2}, {"det_id": "6-2", "class": "person", "class_id": 0, "conf": 0.8445808291435242, "bbox": {"x1": 0.015528382733464241, "y1": 223.8740234375, "x2": 66.65874481201172, "y2": 503.3869323730469}, "bbox_norm": {"x": 1.2131549010518938e-05, "y": 0.31093614366319444, "w": 0.05206501283537364, "h": 0.3882123735215929}, "centroid": {"x": 33, "y": 363}, "centroid_norm": {"x": 0.026044637966697336, "y": 0.5050423304239909}, "track_id": 3}, {"det_id": "6-3", "class": "person", "class_id": 0, "conf": 0.8157052397727966, "bbox": {"x1": 926.5817260742188, "y1": 0.5667365193367004, "x2": 1012.6539306640625, "y2": 214.39117431640625}, "bbox_norm": {"x": 0.7238919734954834, "y": 0.0007871340546343062, "w": 0.06724390983581544, "h": 0.2969783858292633}, "centroid": {"x": 969, "y": 107}, "centroid_norm": {"x": 0.7575139284133912, "y": 0.14927632696926593}, "track_id": 4}], "runtime": {"model_name": "yolo11l.pt", "tracker_type": "botsort", "conf_thres": 0.25, "class_filter": [0]}, "source_uri": "D:\\\\DockerData\\\\retail-video-analytics\\\\vision\\\\video\\\\video3.mp4"}', 'cam_01', 'store_01', datetime.datetime(2025, 11, 22, 13, 10, 6, 237000)], ['v1', None, None, '{"schema_version": "1.0", "pipeline_run_id": "7651a7e53b0e4129be373557ab56378a", "source": {"store_id": "store_01", "camera_id": "cam_01", "stream_id": "stream_01"}, "frame_index": 7, "capture_ts": "2025-11-21T19:48:51.741655+00:00", "image_size": {"width": 1280, "height": 720}, "detections": [{"det_id": "7-0", "class": "person", "class_id": 0, "conf": 0.8786123991012573, "bbox": {"x1": 547.6925048828125, "y1": 208.76039123535156, "x2": 647.6763305664062, "y2": 566.1275634765625}, "bbox_norm": {"x": 0.4278847694396973, "y": 0.28994498782687717, "w": 0.07811236381530762, "h": 0.49634329477945965}, "centroid": {"x": 597, "y": 387}, "centroid_norm": {"x": 0.4669409513473511, "y": 0.538116635216607}, "track_id": 1}, {"det_id": "7-1", "class": "person", "class_id": 0, "conf": 0.8794078230857849, "bbox": {"x1": 715.907958984375, "y1": 105.93514251708984, "x2": 802.2409057617188, "y2": 287.83587646484375}, "bbox_norm": {"x": 0.559303092956543, "y": 0.147132142384847, "w": 0.0674476146697998, "h": 0.2526399082607693}, "centroid": {"x": 759, "y": 196}, "centroid_norm": {"x": 0.5930269002914429, "y": 0.27345209651523167}, "track_id": 2}, {"det_id": "7-2", "class": "person", "class_id": 0, "conf": 0.8561245799064636, "bbox": {"x1": 0.01810748130083084, "y1": 220.796875, "x2": 66.9013442993164, "y2": 503.4162902832031}, "bbox_norm": {"x": 1.4146469766274095e-05, "y": 0.3066623263888889, "w": 0.05225252876407467, "h": 0.39252696567111545}, "centroid": {"x": 33, "y": 362}, "centroid_norm": {"x": 0.02614041085180361, "y": 0.5029258092244466}, "track_id": 3}, {"det_id": "7-3", "class": "person", "class_id": 0, "conf": 0.8116843104362488, "bbox": {"x1": 926.4671020507812, "y1": 0.6592488884925842, "x2": 1012.741455078125, "y2": 214.3695068359375}, "bbox_norm": {"x": 0.7238024234771728, "y": 0.0009156234562397003, "w": 0.0674018383026123, "h": 0.2968198027047846}, "centroid": {"x": 969, "y": 107}, "centroid_norm": {"x": 0.757503342628479, "y": 0.149325524808632}, "track_id": 4}], "runtime": {"model_name": "yolo11l.pt", "tracker_type": "botsort", "conf_thres": 0.25, "class_filter": [0]}, "source_uri": "D:\\\\DockerData\\\\retail-video-analytics\\\\vision\\\\video\\\\video3.mp4"}', 'cam_01', 'store_01', datetime.datetime(2025, 11, 22, 13, 10, 6, 239000)], ['v1', None, None, '{"schema_version": "1.0", "pipeline_run_id": "7651a7e53b0e4129be373557ab56378a", "source": {"store_id": "store_01", "camera_id": "cam_01", "stream_id": "stream_01"}, "frame_index": 8, "capture_ts": "2025-11-21T19:48:51.948722+00:00", "image_size": {"width": 1280, "height": 720}, "detections": [{"det_id": "8-0", "class": "person", "class_id": 0, "conf": 0.8783289790153503, "bbox": {"x1": 547.607666015625, "y1": 209.34466552734375, "x2": 647.32861328125, "y2": 566.1119995117188}, "bbox_norm": {"x": 0.42781848907470704, "y": 0.29075647989908854, "w": 0.07790699005126953, "h": 0.4955101860894097}, "centroid": {"x": 597, "y": 387}, "centroid_norm": {"x": 0.4667719841003418, "y": 0.5385115729437934}, "track_id": 1}, {"det_id": "8-1", "class": "person", "class_id": 0, "conf": 0.8716822266578674, "bbox": {"x1": 711.06005859375, "y1": 105.30859375, "x2": 794.502685546875, "y2": 287.8406677246094}, "bbox_norm": {"x": 0.5555156707763672, "y": 0.14626193576388888, "w": 0.0651895523071289, "h": 0.25351676940917967}, "centroid": {"x": 752, "y": 196}, "centroid_norm": {"x": 0.5881104469299316, "y": 0.27302032046847874}, "track_id": 2}, {"det_id": "8-2", "class": "person", "class_id": 0, "conf": 0.8675758242607117, "bbox": {"x1": 0.21401125192642212, "y1": 220.54322814941406, "x2": 66.28131866455078, "y2": 503.3978271484375}, "bbox_norm": {"x": 0.00016719629056751728, "y": 0.3063100390964084, "w": 0.05161508391611278, "h": 0.39285360972086586}, "centroid": {"x": 33, "y": 361}, "centroid_norm": {"x": 0.025974738248623907, "y": 0.5027368439568414}, "track_id": 3}, {"det_id": "8-3", "class": "person", "class_id": 0, "conf": 0.8104687929153442, "bbox": {"x1": 926.8069458007812, "y1": 0.7208212018013, "x2": 1012.5471801757812, "y2": 214.3749237060547}, "bbox_norm": {"x": 0.7240679264068604, "y": 0.0010011405580573611, "w": 0.06698455810546874, "h": 0.29674180903368524}, "centroid": {"x": 969, "y": 107}, "centroid_norm": {"x": 0.7575602054595947, "y": 0.14937204507489998}, "track_id": 4}], "runtime": {"model_name": "yolo11l.pt", "tracker_type": "botsort", "conf_thres": 0.25, "class_filter": [0]}, "source_uri": "D:\\\\DockerData\\\\retail-video-analytics\\\\vision\\\\video\\\\video3.mp4"}', 'cam_01', 'store_01', datetime.datetime(2025, 11, 22, 13, 10, 6, 240000)], ['v1', None, None, '{"schema_version": "1.0", "pipeline_run_id": "7651a7e53b0e4129be373557ab56378a", "source": {"store_id": "store_01", "camera_id": "cam_01", "stream_id": "stream_01"}, "frame_index": 9, "capture_ts": "2025-11-21T19:48:52.089225+00:00", "image_size": {"width": 1280, "height": 720}, "detections": [{"det_id": "9-0", "class": "person", "class_id": 0, "conf": 0.8772560358047485, "bbox": {"x1": 547.5847778320312, "y1": 209.5325469970703, "x2": 647.196044921875, "y2": 565.9982299804688}, "bbox_norm": {"x": 0.4278006076812744, "y": 0.29101742638481987, "w": 0.07782130241394043, "h": 0.49509122636583114}, "centroid": {"x": 597, "y": 387}, "centroid_norm": {"x": 0.4667112588882446, "y": 0.5385630395677354}, "track_id": 1}, {"det_id": "9-1", "class": "person", "class_id": 0, "conf": 0.8743240833282471, "bbox": {"x1": 709.3777465820312, "y1": 105.11686706542969, "x2": 791.8983154296875, "y2": 287.736083984375}, "bbox_norm": {"x": 0.5542013645172119, "y": 0.14599564870198567, "w": 0.06446919441223145, "h": 0.2536378012763129}, "centroid": {"x": 750, "y": 196}, "centroid_norm": {"x": 0.5864359617233277, "y": 0.27281454934014215}, "track_id": 2}, {"det_id": "9-2", "class": "person", "class_id": 0, "conf": 0.8687391877174377, "bbox": {"x1": 0.28417181968688965, "y1": 220.29408264160156, "x2": 66.04755401611328, "y2": 503.389892578125}, "bbox_norm": {"x": 0.00022200923413038254, "y": 0.30596400366889104, "w": 0.05137764234095812, "h": 0.3931886249118381}, "centroid": {"x": 33, "y": 361}, "centroid_norm": {"x": 0.025910830404609442, "y": 0.5025583161248102}, "track_id": 3}, {"det_id": "9-3", "class": "person", "class_id": 0, "conf": 0.8104916214942932, "bbox": {"x1": 927.3657836914062, "y1": 0.7265313863754272, "x2": 1012.34326171875, "y2": 214.3986053466797}, "bbox_norm": {"x": 0.7245045185089112, "y": 0.0010090713699658711, "w": 0.06638865470886231, "h": 0.29676676938931146}, "centroid": {"x": 969, "y": 107}, "centroid_norm": {"x": 0.7576988458633422, "y": 0.14939245606462162}, "track_id": 4}], "runtime": {"model_name": "yolo11l.pt", "tracker_type": "botsort", "conf_thres": 0.25, "class_filter": [0]}, "source_uri": "D:\\\\DockerData\\\\retail-video-analytics\\\\vision\\\\video\\\\video3.mp4"}', 'cam_01', 'store_01', datetime.datetime(2025, 11, 22, 13, 10, 6, 241000)], ['v1', None, None, '{"schema_version": "1.0", "pipeline_run_id": "7651a7e53b0e4129be373557ab56378a", "source": {"store_id": "store_01", "camera_id": "cam_01", "stream_id": "stream_01"}, "frame_index": 10, "capture_ts": "2025-11-21T19:48:52.266978+00:00", "image_size": {"width": 1280, "height": 720}, "detections": [{"det_id": "10-0", "class": "person", "class_id": 0, "conf": 0.8771692514419556, "bbox": {"x1": 547.5553588867188, "y1": 209.56556701660156, "x2": 647.150390625, "y2": 565.9588012695312}, "bbox_norm": {"x": 0.427777624130249, "y": 0.2910632875230577, "w": 0.07780861854553223, "h": 0.494990603129069}, "centroid": {"x": 597, "y": 387}, "centroid_norm": {"x": 0.4666819334030151, "y": 0.5385585890875922}, "track_id": 1}, {"det_id": "10-1", "class": "person", "class_id": 0, "conf": 0.8714913725852966, "bbox": {"x1": 708.8274536132812, "y1": 105.06568908691406, "x2": 791.157958984375, "y2": 287.6654357910156}, "bbox_norm": {"x": 0.5537714481353759, "y": 0.14592456817626953, "w": 0.06432070732116699, "h": 0.25361075931125215}, "centroid": {"x": 749, "y": 196}, "centroid_norm": {"x": 0.5859318017959595, "y": 0.27272994783189564}, "track_id": 2}, {"det_id": "10-2", "class": "person", "class_id": 0, "conf": 0.8681414723396301, "bbox": {"x1": 0.30784791707992554, "y1": 220.45492553710938, "x2": 65.97663879394531, "y2": 503.3869323730469}, "bbox_norm": {"x": 0.00024050618521869182, "y": 0.3061873965793186, "w": 0.051303742872551086, "h": 0.39296112060546873}, "centroid": {"x": 33, "y": 361}, "centroid_norm": {"x": 0.025892377621494232, "y": 0.5026679568820529}, "track_id": 3}, {"det_id": "10-3", "class": "person", "class_id": 0, "conf": 0.810215950012207, "bbox": {"x1": 927.555419921875, "y1": 0.6553019285202026, "x2": 1012.35791015625, "y2": 214.3892822265625}, "bbox_norm": {"x": 0.7246526718139649, "y": 0.0009101415673891703, "w": 0.06625194549560547, "h": 0.29685275041394765}, "centroid": {"x": 969, "y": 107}, "centroid_norm": {"x": 0.7577786445617676, "y": 0.149336516774363}, "track_id": 4}], "runtime": {"model_name": "yolo11l.pt", "tracker_type": "botsort", "conf_thres": 0.25, "class_filter": [0]}, "source_uri": "D:\\\\DockerData\\\\retail-video-analytics\\\\vision\\\\video\\\\video3.mp4"}', 'cam_01', 'store_01', datetime.datetime(2025, 11, 22, 13, 10, 6, 242000)]]
    

#### Kiểm tra cấu trúc bảng (Schema)


```python
cursor.execute("DESCRIBE bronze_raw")
columns = cursor.fetchall()
print("Cấu trúc bảng bronze_raw:")
pd.DataFrame(columns, columns=['Column', 'Type', 'Extra', 'Comment'])
```

    Cấu trúc bảng bronze_raw:
    




<div>
<style scoped>
    .dataframe tbody tr th:only-of-type {
        vertical-align: middle;
    }

    .dataframe tbody tr th {
        vertical-align: top;
    }

    .dataframe thead th {
        text-align: right;
    }
</style>
<table border="1" class="dataframe">
  <thead>
    <tr style="text-align: right;">
      <th></th>
      <th>Column</th>
      <th>Type</th>
      <th>Extra</th>
      <th>Comment</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <th>0</th>
      <td>schema_version</td>
      <td>varchar</td>
      <td></td>
      <td></td>
    </tr>
    <tr>
      <th>1</th>
      <td>pipeline_run_id</td>
      <td>varchar</td>
      <td></td>
      <td></td>
    </tr>
    <tr>
      <th>2</th>
      <td>frame_index</td>
      <td>bigint</td>
      <td></td>
      <td></td>
    </tr>
    <tr>
      <th>3</th>
      <td>payload</td>
      <td>varchar</td>
      <td></td>
      <td></td>
    </tr>
    <tr>
      <th>4</th>
      <td>camera_id</td>
      <td>varchar</td>
      <td></td>
      <td></td>
    </tr>
    <tr>
      <th>5</th>
      <td>store_id</td>
      <td>varchar</td>
      <td></td>
      <td></td>
    </tr>
    <tr>
      <th>6</th>
      <td>ingest_ts</td>
      <td>timestamp(6)</td>
      <td></td>
      <td></td>
    </tr>
  </tbody>
</table>
</div>



#### Số lượng record trong Bronze


```python

cursor.execute("SELECT COUNT(*) FROM bronze_raw")
rows = cursor.fetchall()
print(rows)

```

    [[317]]
    

#### Xem schema bảng bronze_raw


```python
cursor.execute("DESCRIBE bronze_raw")
schema_rows = cursor.fetchall()
for r in schema_rows:
    print(r)

```

    ['schema_version', 'varchar', '', '']
    ['pipeline_run_id', 'varchar', '', '']
    ['frame_index', 'bigint', '', '']
    ['payload', 'varchar', '', '']
    ['camera_id', 'varchar', '', '']
    ['store_id', 'varchar', '', '']
    ['ingest_ts', 'timestamp(6)', '', '']
    

1) schema_version (varchar)

Phiên bản schema của metadata JSON từ module vision.
Giúp bạn biết lúc nào format JSON thay đổi.
Ví dụ: "v1".

2) pipeline_run_id (varchar)

ID duy nhất cho mỗi lần chạy vision pipeline.
Một video chạy lại → một pipeline_run_id khác.
Dùng để debug hoặc trace theo từng lần chạy.

3) frame_index (bigint)

Số thứ tự frame trong video.
Ví dụ: 1, 2, 3, 4…
Quan trọng cho tracking thời gian thực.

4) payload (varchar)

JSON thô từ vision.
Đây là phần to nhất: detections, bbox, timestamp, centroid…
Silver sẽ parse từ đây.

5) camera_id (varchar)

ID camera gửi dữ liệu (ví dụ: "cam_01").
Bạn dùng để partition/cluster data theo camera.

6) store_id (varchar)

ID cửa hàng (ví dụ "store_01").
Giúp truy vấn BI theo từng cửa hàng.

7) ingest_ts (timestamp(6))

Thời điểm Flink ghi record vào Iceberg.
Không phải capture_ts từ JSON → mà là timestamp ingestion.
Dùng để kiểm tra trễ, latency pipeline.

---


```python
cursor.execute("""
SELECT 
    json_extract(payload, '$.capture_ts') AS ts,
    json_extract(payload, '$.detections') AS dets
FROM bronze_raw
LIMIT 3
""")
print(cursor.fetchall())

```

    [['"2025-11-21T19:48:50.907551+00:00"', '[{"det_id":"1-0","class":"person","class_id":0,"conf":0.879061222076416,"bbox":{"x1":547.5609130859375,"y1":208.7528076171875,"x2":647.697265625,"y2":566.1458740234375},"bbox_norm":{"x":0.4277819633483887,"y":0.28993445502387155,"w":0.07823152542114258,"h":0.49637925889756945},"centroid":{"x":597,"y":387},"centroid_norm":{"x":0.46689772605895996,"y":0.5381240844726562},"track_id":1},{"det_id":"1-1","class":"person","class_id":0,"conf":0.8788681030273438,"bbox":{"x1":720.4228515625,"y1":106.02847290039062,"x2":808.581298828125,"y2":287.6534729003906},"bbox_norm":{"x":0.5628303527832031,"y":0.1472617679172092,"w":0.06887378692626953,"h":0.25225694444444446},"centroid":{"x":764,"y":196},"centroid_norm":{"x":0.5972672462463379,"y":0.2733902401394314},"track_id":2},{"det_id":"1-2","class":"person","class_id":0,"conf":0.8445228338241577,"bbox":{"x1":0.01546478271484375,"y1":223.85882568359375,"x2":66.65916442871094,"y2":503.38714599609375},"bbox_norm":{"x":1.2081861495971679E-5,"y":0.310915035671658,"w":0.05206539034843445,"h":0.38823377821180555},"centroid":{"x":33,"y":363},"centroid_norm":{"x":0.026044777035713194,"y":0.5050319247775608},"track_id":3},{"det_id":"1-3","class":"person","class_id":0,"conf":0.8155996799468994,"bbox":{"x1":926.5863037109375,"y1":0.5677490234375,"x2":1012.654052734375,"y2":214.39083862304688},"bbox_norm":{"x":0.7238955497741699,"y":7.885403103298611E-4,"w":0.06724042892456054,"h":0.2969765133327908},"centroid":{"x":969,"y":107},"centroid_norm":{"x":0.7575157642364502,"y":0.14927679697672527},"track_id":4}]'], ['"2025-11-21T19:48:51.165038+00:00"', '[{"det_id":"2-0","class":"person","class_id":0,"conf":0.8791031241416931,"bbox":{"x1":547.560791015625,"y1":208.7517547607422,"x2":647.6970825195312,"y2":566.1426391601562},"bbox_norm":{"x":0.427781867980957,"y":0.289932992723253,"w":0.07823147773742675,"h":0.49637622833251954},"centroid":{"x":597,"y":387},"centroid_norm":{"x":0.4668976068496704,"y":0.5381211068895128},"track_id":1},{"det_id":"2-1","class":"person","class_id":0,"conf":0.8789587020874023,"bbox":{"x1":720.4228515625,"y1":106.02108764648438,"x2":808.5772705078125,"y2":287.6536560058594},"bbox_norm":{"x":0.5628303527832031,"y":0.14725151062011718,"w":0.0688706398010254,"h":0.2522674560546875},"centroid":{"x":764,"y":196},"centroid_norm":{"x":0.5972656726837158,"y":0.27338523864746095},"track_id":2},{"det_id":"2-2","class":"person","class_id":0,"conf":0.8445611596107483,"bbox":{"x1":0.0154449213296175,"y1":223.8511962890625,"x2":66.65782928466797,"y2":503.38836669921875},"bbox_norm":{"x":1.2066344788763672E-5,"y":0.3109044392903646,"w":0.052064362783858086,"h":0.3882460700141059},"centroid":{"x":33,"y":363},"centroid_norm":{"x":0.026044247736717808,"y":0.5050274742974176},"track_id":3},{"det_id":"2-3","class":"person","class_id":0,"conf":0.8156672120094299,"bbox":{"x1":926.5841674804688,"y1":0.566967785358429,"x2":1012.6541748046875,"y2":214.39312744140625},"bbox_norm":{"x":0.7238938808441162,"y":7.874552574422624E-4,"w":0.0672421932220459,"h":0.29698077730006645},"centroid":{"x":969,"y":107},"centroid_norm":{"x":0.7575149774551392,"y":0.14927784390747548},"track_id":4}]'], ['"2025-11-21T19:48:51.270014+00:00"', '[{"det_id":"3-0","class":"person","class_id":0,"conf":0.8791031241416931,"bbox":{"x1":547.560791015625,"y1":208.75157165527344,"x2":647.697021484375,"y2":566.14208984375},"bbox_norm":{"x":0.427781867980957,"y":0.289932738410102,"w":0.07823143005371094,"h":0.49637571970621747},"centroid":{"x":597,"y":387},"centroid_norm":{"x":0.4668975830078125,"y":0.5381205982632107},"track_id":1},{"det_id":"3-1","class":"person","class_id":0,"conf":0.8789587020874023,"bbox":{"x1":720.4228515625,"y1":106.01982116699219,"x2":808.5765991210938,"y2":287.6536865234375},"bbox_norm":{"x":0.5628303527832031,"y":0.1472497516208225,"w":0.06887011528015137,"h":0.2522692574395074},"centroid":{"x":764,"y":196},"centroid_norm":{"x":0.5972654104232789,"y":0.27338438034057616},"track_id":2},{"det_id":"3-2","class":"person","class_id":0,"conf":0.8445611596107483,"bbox":{"x1":0.01544151920825243,"y1":223.8498992919922,"x2":66.65760040283203,"y2":503.3885803222656},"bbox_norm":{"x":1.2063686881447211E-5,"y":0.31090263790554473,"w":0.05206418662783108,"h":0.388248168097602},"centroid":{"x":33,"y":363},"centroid_norm":{"x":0.026044157000796986,"y":0.5050267219543457},"track_id":3},{"det_id":"3-3","class":"person","class_id":0,"conf":0.8156672120094299,"bbox":{"x1":926.5838012695312,"y1":0.5668339729309082,"x2":1012.6541748046875,"y2":214.39352416992188},"bbox_norm":{"x":0.7238935947418212,"y":7.872694068484837E-4,"w":0.06724247932434083,"h":0.29698151416248747},"centroid":{"x":969,"y":107},"centroid_norm":{"x":0.7575148344039917,"y":0.1492780264880922},"track_id":4}]']]
    

### Lớp silver

#### Lấy capture_ts + số người / frame


```python
cursor.execute("""
SELECT
    json_extract_scalar(payload, '$.frame_index') AS frame_index,
    json_extract_scalar(payload, '$.capture_ts') AS capture_ts_str,
    json_array_length(json_extract(payload, '$.detections')) AS num_person,
    camera_id,
    store_id,
    ingest_ts
FROM bronze_raw
LIMIT 5
""")
rows = cursor.fetchall()
for r in rows:
    print(r)

```

    ['2', '2025-11-21T19:48:51.165038+00:00', 4, 'cam_01', 'store_01', datetime.datetime(2025, 11, 22, 13, 10, 6, 230000)]
    ['3', '2025-11-21T19:48:51.270014+00:00', 4, 'cam_01', 'store_01', datetime.datetime(2025, 11, 22, 13, 10, 6, 233000)]
    ['1', '2025-11-21T19:48:50.907551+00:00', 4, 'cam_01', 'store_01', datetime.datetime(2025, 11, 22, 13, 10, 5, 546000)]
    ['4', '2025-11-21T19:48:51.378599+00:00', 4, 'cam_01', 'store_01', datetime.datetime(2025, 11, 22, 13, 10, 6, 235000)]
    ['5', '2025-11-21T19:48:51.473212+00:00', 4, 'cam_01', 'store_01', datetime.datetime(2025, 11, 22, 13, 10, 6, 236000)]
    

#### Bung mảng detections thành từng dòng (UNNEST)


```python
cursor.execute("""
SELECT
    CAST(json_extract_scalar(payload, '$.frame_index') AS bigint) AS frame_index,
    from_iso8601_timestamp(json_extract_scalar(payload, '$.capture_ts')) AS capture_ts,
    camera_id,
    store_id,
    json_extract_scalar(det, '$.det_id') AS det_id,
    json_extract_scalar(det, '$.class') AS class,
    CAST(json_extract_scalar(det, '$.class_id') AS integer) AS class_id,
    CAST(json_extract_scalar(det, '$.conf') AS double) AS conf,
    CAST(json_extract_scalar(det, '$.bbox.x1') AS double) AS bbox_x1,
    CAST(json_extract_scalar(det, '$.bbox.y1') AS double) AS bbox_y1,
    CAST(json_extract_scalar(det, '$.bbox.x2') AS double) AS bbox_x2,
    CAST(json_extract_scalar(det, '$.bbox.y2') AS double) AS bbox_y2,
    CAST(json_extract_scalar(det, '$.centroid.x') AS double) AS centroid_x,
    CAST(json_extract_scalar(det, '$.centroid.y') AS double) AS centroid_y,
    CAST(json_extract_scalar(det, '$.track_id') AS bigint) AS track_id,
    ingest_ts
FROM bronze_raw
CROSS JOIN UNNEST(
    CAST(json_extract(payload, '$.detections') AS array(json))
) AS t(det)
LIMIT 10
""")
rows = cursor.fetchall()
for r in rows:
    print(r)

```

    [1, datetime.datetime(2025, 11, 21, 19, 48, 50, 907000, tzinfo=zoneinfo.ZoneInfo(key='UTC')), 'cam_01', 'store_01', '1-0', 'person', 0, 0.879061222076416, 547.5609130859375, 208.7528076171875, 647.697265625, 566.1458740234375, 597.0, 387.0, 1, datetime.datetime(2025, 11, 22, 13, 10, 5, 546000)]
    [1, datetime.datetime(2025, 11, 21, 19, 48, 50, 907000, tzinfo=zoneinfo.ZoneInfo(key='UTC')), 'cam_01', 'store_01', '1-1', 'person', 0, 0.8788681030273438, 720.4228515625, 106.02847290039062, 808.581298828125, 287.6534729003906, 764.0, 196.0, 2, datetime.datetime(2025, 11, 22, 13, 10, 5, 546000)]
    [1, datetime.datetime(2025, 11, 21, 19, 48, 50, 907000, tzinfo=zoneinfo.ZoneInfo(key='UTC')), 'cam_01', 'store_01', '1-2', 'person', 0, 0.8445228338241577, 0.01546478271484375, 223.85882568359375, 66.65916442871094, 503.38714599609375, 33.0, 363.0, 3, datetime.datetime(2025, 11, 22, 13, 10, 5, 546000)]
    [1, datetime.datetime(2025, 11, 21, 19, 48, 50, 907000, tzinfo=zoneinfo.ZoneInfo(key='UTC')), 'cam_01', 'store_01', '1-3', 'person', 0, 0.8155996799468994, 926.5863037109375, 0.5677490234375, 1012.654052734375, 214.39083862304688, 969.0, 107.0, 4, datetime.datetime(2025, 11, 22, 13, 10, 5, 546000)]
    [2, datetime.datetime(2025, 11, 21, 19, 48, 51, 165000, tzinfo=zoneinfo.ZoneInfo(key='UTC')), 'cam_01', 'store_01', '2-0', 'person', 0, 0.8791031241416931, 547.560791015625, 208.7517547607422, 647.6970825195312, 566.1426391601562, 597.0, 387.0, 1, datetime.datetime(2025, 11, 22, 13, 10, 6, 230000)]
    [2, datetime.datetime(2025, 11, 21, 19, 48, 51, 165000, tzinfo=zoneinfo.ZoneInfo(key='UTC')), 'cam_01', 'store_01', '2-1', 'person', 0, 0.8789587020874023, 720.4228515625, 106.02108764648438, 808.5772705078125, 287.6536560058594, 764.0, 196.0, 2, datetime.datetime(2025, 11, 22, 13, 10, 6, 230000)]
    [2, datetime.datetime(2025, 11, 21, 19, 48, 51, 165000, tzinfo=zoneinfo.ZoneInfo(key='UTC')), 'cam_01', 'store_01', '2-2', 'person', 0, 0.8445611596107483, 0.0154449213296175, 223.8511962890625, 66.65782928466797, 503.38836669921875, 33.0, 363.0, 3, datetime.datetime(2025, 11, 22, 13, 10, 6, 230000)]
    [2, datetime.datetime(2025, 11, 21, 19, 48, 51, 165000, tzinfo=zoneinfo.ZoneInfo(key='UTC')), 'cam_01', 'store_01', '2-3', 'person', 0, 0.8156672120094299, 926.5841674804688, 0.566967785358429, 1012.6541748046875, 214.39312744140625, 969.0, 107.0, 4, datetime.datetime(2025, 11, 22, 13, 10, 6, 230000)]
    [3, datetime.datetime(2025, 11, 21, 19, 48, 51, 270000, tzinfo=zoneinfo.ZoneInfo(key='UTC')), 'cam_01', 'store_01', '3-0', 'person', 0, 0.8791031241416931, 547.560791015625, 208.75157165527344, 647.697021484375, 566.14208984375, 597.0, 387.0, 1, datetime.datetime(2025, 11, 22, 13, 10, 6, 233000)]
    [3, datetime.datetime(2025, 11, 21, 19, 48, 51, 270000, tzinfo=zoneinfo.ZoneInfo(key='UTC')), 'cam_01', 'store_01', '3-1', 'person', 0, 0.8789587020874023, 720.4228515625, 106.01982116699219, 808.5765991210938, 287.6536865234375, 764.0, 196.0, 2, datetime.datetime(2025, 11, 22, 13, 10, 6, 233000)]
    

#### Tạo bảng Silver trong Iceberg (Trino)


```python
cursor.execute("""
CREATE TABLE silver_detections_v2 (
    frame_index bigint,
    capture_ts timestamp(6),
    camera_id varchar,
    store_id varchar,
    det_id varchar,
    class varchar,
    class_id integer,
    conf double,
    bbox_x1 double,
    bbox_y1 double,
    bbox_x2 double,
    bbox_y2 double,
    centroid_x double,
    centroid_y double,
    track_id bigint,
    ingest_ts timestamp(6)
)
WITH (
    format = 'PARQUET'
)
""")
print("created silver_detections_v2")

```

    created silver_detections_v2
    

#### Insert dữ liệu từ Bronze → Silver


```python
cursor.execute("""
INSERT INTO silver_detections_v2
SELECT
    CAST(json_extract_scalar(payload, '$.frame_index') AS bigint) AS frame_index,
    CAST(from_iso8601_timestamp(json_extract_scalar(payload, '$.capture_ts')) AS timestamp(6)) AS capture_ts,
    camera_id,
    store_id,
    json_extract_scalar(det, '$.det_id') AS det_id,
    json_extract_scalar(det, '$.class') AS class,
    CAST(json_extract_scalar(det, '$.class_id') AS integer) AS class_id,
    CAST(json_extract_scalar(det, '$.conf') AS double) AS conf,
    CAST(json_extract_scalar(det, '$.bbox.x1') AS double) AS bbox_x1,
    CAST(json_extract_scalar(det, '$.bbox.y1') AS double) AS bbox_y1,
    CAST(json_extract_scalar(det, '$.bbox.x2') AS double) AS bbox_x2,
    CAST(json_extract_scalar(det, '$.bbox.y2') AS double) AS bbox_y2,
    CAST(json_extract_scalar(det, '$.centroid.x') AS double) AS centroid_x,
    CAST(json_extract_scalar(det, '$.centroid.y') AS double) AS centroid_y,
    CAST(json_extract_scalar(det, '$.track_id') AS bigint) AS track_id,
    CAST(ingest_ts AS timestamp(6)) AS ingest_ts
FROM bronze_raw
CROSS JOIN UNNEST(
    CAST(json_extract(payload, '$.detections') AS array(json))
) AS t(det)
""")
print("inserted into silver_detections_v2")

```

    inserted into silver_detections_v2
    

#### Kiểm tra lại


```python
cursor.execute("SELECT COUNT(*) FROM silver_detections_v2")
print(cursor.fetchall())

cursor.execute("""
SELECT * FROM silver_detections_v2
ORDER BY capture_ts
LIMIT 10
""")
print(cursor.fetchall())

```

    [[1499]]
    [[1, datetime.datetime(2025, 11, 21, 19, 48, 50, 907000), 'cam_01', 'store_01', '1-2', 'person', 0, 0.8445228338241577, 0.01546478271484375, 223.85882568359375, 66.65916442871094, 503.38714599609375, 33.0, 363.0, 3, datetime.datetime(2025, 11, 22, 13, 10, 5, 546000)], [1, datetime.datetime(2025, 11, 21, 19, 48, 50, 907000), 'cam_01', 'store_01', '1-0', 'person', 0, 0.879061222076416, 547.5609130859375, 208.7528076171875, 647.697265625, 566.1458740234375, 597.0, 387.0, 1, datetime.datetime(2025, 11, 22, 13, 10, 5, 546000)], [1, datetime.datetime(2025, 11, 21, 19, 48, 50, 907000), 'cam_01', 'store_01', '1-1', 'person', 0, 0.8788681030273438, 720.4228515625, 106.02847290039062, 808.581298828125, 287.6534729003906, 764.0, 196.0, 2, datetime.datetime(2025, 11, 22, 13, 10, 5, 546000)], [1, datetime.datetime(2025, 11, 21, 19, 48, 50, 907000), 'cam_01', 'store_01', '1-3', 'person', 0, 0.8155996799468994, 926.5863037109375, 0.5677490234375, 1012.654052734375, 214.39083862304688, 969.0, 107.0, 4, datetime.datetime(2025, 11, 22, 13, 10, 5, 546000)], [2, datetime.datetime(2025, 11, 21, 19, 48, 51, 165000), 'cam_01', 'store_01', '2-2', 'person', 0, 0.8445611596107483, 0.0154449213296175, 223.8511962890625, 66.65782928466797, 503.38836669921875, 33.0, 363.0, 3, datetime.datetime(2025, 11, 22, 13, 10, 6, 230000)], [2, datetime.datetime(2025, 11, 21, 19, 48, 51, 165000), 'cam_01', 'store_01', '2-3', 'person', 0, 0.8156672120094299, 926.5841674804688, 0.566967785358429, 1012.6541748046875, 214.39312744140625, 969.0, 107.0, 4, datetime.datetime(2025, 11, 22, 13, 10, 6, 230000)], [2, datetime.datetime(2025, 11, 21, 19, 48, 51, 165000), 'cam_01', 'store_01', '2-0', 'person', 0, 0.8791031241416931, 547.560791015625, 208.7517547607422, 647.6970825195312, 566.1426391601562, 597.0, 387.0, 1, datetime.datetime(2025, 11, 22, 13, 10, 6, 230000)], [2, datetime.datetime(2025, 11, 21, 19, 48, 51, 165000), 'cam_01', 'store_01', '2-1', 'person', 0, 0.8789587020874023, 720.4228515625, 106.02108764648438, 808.5772705078125, 287.6536560058594, 764.0, 196.0, 2, datetime.datetime(2025, 11, 22, 13, 10, 6, 230000)], [3, datetime.datetime(2025, 11, 21, 19, 48, 51, 270000), 'cam_01', 'store_01', '3-0', 'person', 0, 0.8791031241416931, 547.560791015625, 208.75157165527344, 647.697021484375, 566.14208984375, 597.0, 387.0, 1, datetime.datetime(2025, 11, 22, 13, 10, 6, 233000)], [3, datetime.datetime(2025, 11, 21, 19, 48, 51, 270000), 'cam_01', 'store_01', '3-1', 'person', 0, 0.8789587020874023, 720.4228515625, 106.01982116699219, 808.5765991210938, 287.6536865234375, 764.0, 196.0, 2, datetime.datetime(2025, 11, 22, 13, 10, 6, 233000)]]
    

#### Khám phá dữ liệu (silver)

##### 1.1 – Tổng số detection, frame, track


```python
cursor.execute("""
SELECT
    COUNT(*) AS total_detections,
    COUNT(DISTINCT frame_index) AS total_frames,
    COUNT(DISTINCT track_id) AS total_tracks
FROM silver_detections_v2
""")
print(cursor.fetchall())

```

    [[1499, 317, 8]]
    

##### 1.2 – Kiểm tra missing frame (frame bị nhảy)


```python
cursor.execute("""
WITH f AS (
    SELECT DISTINCT frame_index
    FROM silver_detections_v2
)
SELECT
    MIN(frame_index) AS min_frame,
    MAX(frame_index) AS max_frame,
    COUNT(*) AS frame_count,
    (MAX(frame_index) - MIN(frame_index) + 1) AS expected_frames,
    (MAX(frame_index) - MIN(frame_index) + 1) - COUNT(*) AS missing_frames
FROM f
""")
print(cursor.fetchall())

```

    [[1, 317, 317, 317, 0]]
    

#### 1.3 – Kiểm tra số người mỗi frame (để xem có outlier)


```python
cursor.execute("""
SELECT
    frame_index,
    COUNT(*) AS num_person
FROM silver_detections_v2
GROUP BY frame_index
ORDER BY frame_index
LIMIT 20
""")
print(cursor.fetchall())

```

    [[1, 4], [2, 4], [3, 4], [4, 4], [5, 4], [6, 4], [7, 4], [8, 4], [9, 4], [10, 4], [11, 4], [12, 4], [13, 4], [14, 4], [15, 4], [16, 4], [17, 4], [18, 4], [19, 4], [20, 4]]
    

#### 1.4 – Kiểm tra phân bố confidence (độ tự tin của YOLO)


```python
cursor.execute("""
SELECT
    approx_percentile(conf, 0.1) AS p10,
    approx_percentile(conf, 0.5) AS p50,
    approx_percentile(conf, 0.9) AS p90
FROM silver_detections_v2
""")
print(cursor.fetchall())

```

    [[0.6662853403271032, 0.8361939502049104, 0.8869955837726593]]
    

##### 1.5 – Kiểm tra timestamp đều hay không


```python
cursor.execute("""
WITH t AS (
    SELECT
        capture_ts,
        LAG(capture_ts) OVER (ORDER BY capture_ts) AS prev_ts,
        ROW_NUMBER() OVER (ORDER BY capture_ts) AS rn
    FROM silver_detections_v2
)
SELECT
    capture_ts,
    prev_ts,
    (capture_ts - prev_ts) AS gap
FROM t
WHERE rn <= 20
""")
print(cursor.fetchall())

```

    [[datetime.datetime(2025, 11, 21, 19, 48, 50, 907000), datetime.datetime(2025, 11, 21, 19, 48, 50, 907000), datetime.timedelta(0)], [datetime.datetime(2025, 11, 21, 19, 48, 50, 907000), None, None], [datetime.datetime(2025, 11, 21, 19, 48, 50, 907000), datetime.datetime(2025, 11, 21, 19, 48, 50, 907000), datetime.timedelta(0)], [datetime.datetime(2025, 11, 21, 19, 48, 50, 907000), datetime.datetime(2025, 11, 21, 19, 48, 50, 907000), datetime.timedelta(0)], [datetime.datetime(2025, 11, 21, 19, 48, 51, 165000), datetime.datetime(2025, 11, 21, 19, 48, 51, 165000), datetime.timedelta(0)], [datetime.datetime(2025, 11, 21, 19, 48, 51, 165000), datetime.datetime(2025, 11, 21, 19, 48, 51, 165000), datetime.timedelta(0)], [datetime.datetime(2025, 11, 21, 19, 48, 51, 165000), datetime.datetime(2025, 11, 21, 19, 48, 51, 165000), datetime.timedelta(0)], [datetime.datetime(2025, 11, 21, 19, 48, 51, 165000), datetime.datetime(2025, 11, 21, 19, 48, 50, 907000), datetime.timedelta(microseconds=258000)], [datetime.datetime(2025, 11, 21, 19, 48, 51, 270000), datetime.datetime(2025, 11, 21, 19, 48, 51, 270000), datetime.timedelta(0)], [datetime.datetime(2025, 11, 21, 19, 48, 51, 270000), datetime.datetime(2025, 11, 21, 19, 48, 51, 165000), datetime.timedelta(microseconds=105000)], [datetime.datetime(2025, 11, 21, 19, 48, 51, 270000), datetime.datetime(2025, 11, 21, 19, 48, 51, 270000), datetime.timedelta(0)], [datetime.datetime(2025, 11, 21, 19, 48, 51, 270000), datetime.datetime(2025, 11, 21, 19, 48, 51, 270000), datetime.timedelta(0)], [datetime.datetime(2025, 11, 21, 19, 48, 51, 378000), datetime.datetime(2025, 11, 21, 19, 48, 51, 378000), datetime.timedelta(0)], [datetime.datetime(2025, 11, 21, 19, 48, 51, 378000), datetime.datetime(2025, 11, 21, 19, 48, 51, 378000), datetime.timedelta(0)], [datetime.datetime(2025, 11, 21, 19, 48, 51, 378000), datetime.datetime(2025, 11, 21, 19, 48, 51, 270000), datetime.timedelta(microseconds=108000)], [datetime.datetime(2025, 11, 21, 19, 48, 51, 378000), datetime.datetime(2025, 11, 21, 19, 48, 51, 378000), datetime.timedelta(0)], [datetime.datetime(2025, 11, 21, 19, 48, 51, 473000), datetime.datetime(2025, 11, 21, 19, 48, 51, 473000), datetime.timedelta(0)], [datetime.datetime(2025, 11, 21, 19, 48, 51, 473000), datetime.datetime(2025, 11, 21, 19, 48, 51, 473000), datetime.timedelta(0)], [datetime.datetime(2025, 11, 21, 19, 48, 51, 473000), datetime.datetime(2025, 11, 21, 19, 48, 51, 473000), datetime.timedelta(0)], [datetime.datetime(2025, 11, 21, 19, 48, 51, 473000), datetime.datetime(2025, 11, 21, 19, 48, 51, 378000), datetime.timedelta(microseconds=95000)]]
    

#### Bước 2 – Phân tích track_id (hành vi từng người)

##### 2.1. Thống kê mỗi track_id


```python
cursor.execute("""
SELECT
    track_id,
    COUNT(*) AS frames_visible,
    MIN(capture_ts) AS start_time,
    MAX(capture_ts) AS end_time,
    MAX(capture_ts) - MIN(capture_ts) AS duration
FROM silver_detections_v2
GROUP BY track_id
ORDER BY track_id
""")
print(cursor.fetchall())

```

    [[1, 317, datetime.datetime(2025, 11, 21, 19, 48, 50, 907000), datetime.datetime(2025, 11, 21, 19, 49, 25, 843000), datetime.timedelta(seconds=34, microseconds=936000)], [2, 26, datetime.datetime(2025, 11, 21, 19, 48, 50, 907000), datetime.datetime(2025, 11, 21, 19, 48, 54, 649000), datetime.timedelta(seconds=3, microseconds=742000)], [3, 311, datetime.datetime(2025, 11, 21, 19, 48, 50, 907000), datetime.datetime(2025, 11, 21, 19, 49, 25, 301000), datetime.timedelta(seconds=34, microseconds=394000)], [4, 317, datetime.datetime(2025, 11, 21, 19, 48, 50, 907000), datetime.datetime(2025, 11, 21, 19, 49, 25, 843000), datetime.timedelta(seconds=34, microseconds=936000)], [5, 15, datetime.datetime(2025, 11, 21, 19, 48, 54, 695000), datetime.datetime(2025, 11, 21, 19, 48, 55, 527000), datetime.timedelta(microseconds=832000)], [6, 280, datetime.datetime(2025, 11, 21, 19, 48, 55, 346000), datetime.datetime(2025, 11, 21, 19, 49, 25, 843000), datetime.timedelta(seconds=30, microseconds=497000)], [7, 231, datetime.datetime(2025, 11, 21, 19, 48, 59, 330000), datetime.datetime(2025, 11, 21, 19, 49, 25, 843000), datetime.timedelta(seconds=26, microseconds=513000)], [8, 2, datetime.datetime(2025, 11, 21, 19, 49, 25, 778000), datetime.datetime(2025, 11, 21, 19, 49, 25, 843000), datetime.timedelta(microseconds=65000)]]
    

##### 2.2. Vị trí trung bình mỗi người (để sau này dựa vào làm heatmap / zone)


```python
cursor.execute("""
SELECT
    track_id,
    AVG(centroid_x) AS avg_x,
    AVG(centroid_y) AS avg_y
FROM silver_detections_v2
GROUP BY track_id
ORDER BY track_id
""")
print(cursor.fetchall())

```

    [[1, 382.3659305993691, 392.8706624605678], [2, 730.0384615384615, 207.34615384615384], [3, 35.430868167202576, 368.16720257234726], [4, 967.9873817034701, 106.93690851735016], [5, 108.66666666666667, 178.6], [6, 319.39285714285717, 196.60357142857143], [7, 126.95238095238095, 224.03896103896105], [8, 170.0, 152.5]]
    

##### 2.3. Di chuyển (độ lệch x, y) mỗi track


```python
cursor.execute("""
SELECT
    track_id,
    MIN(centroid_x) AS min_x,
    MAX(centroid_x) AS max_x,
    MAX(centroid_x) - MIN(centroid_x) AS delta_x,
    MIN(centroid_y) AS min_y,
    MAX(centroid_y) AS max_y,
    MAX(centroid_y) - MIN(centroid_y) AS delta_y
FROM silver_detections_v2
GROUP BY track_id
ORDER BY track_id
""")
print(cursor.fetchall())

```

    [[1, 108.0, 597.0, 489.0, 335.0, 465.0, 130.0], [2, 682.0, 764.0, 82.0, 196.0, 224.0, 28.0], [3, 27.0, 47.0, 20.0, 351.0, 476.0, 125.0], [4, 956.0, 990.0, 34.0, 105.0, 108.0, 3.0], [5, 105.0, 113.0, 8.0, 172.0, 187.0, 15.0], [6, 132.0, 636.0, 504.0, 137.0, 234.0, 97.0], [7, 91.0, 168.0, 77.0, 173.0, 278.0, 105.0], [8, 170.0, 170.0, 0.0, 152.0, 153.0, 1.0]]
    

#### BƯỚC 3 – TẠO HEATMAP

##### 3.1 – SQL tạo heatmap grid 10×10


```python
cursor.execute("""
WITH grid AS (
    SELECT
        floor(centroid_x / (1280 / 10)) AS gx,
        floor(centroid_y / (720 / 10)) AS gy
    FROM silver_detections_v2
)
SELECT
    gx, gy,
    COUNT(*) AS hits
FROM grid
GROUP BY gx, gy
ORDER BY gy, gx
""")
rows = cursor.fetchall()
for r in rows:
    print(r)

```

    [4.0, 1.0, 6]
    [7.0, 1.0, 317]
    [0.0, 2.0, 66]
    [1.0, 2.0, 111]
    [2.0, 2.0, 101]
    [3.0, 2.0, 43]
    [4.0, 2.0, 13]
    [5.0, 2.0, 16]
    [0.0, 3.0, 97]
    [1.0, 3.0, 74]
    [2.0, 3.0, 17]
    [5.0, 3.0, 10]
    [0.0, 4.0, 144]
    [1.0, 4.0, 58]
    [0.0, 5.0, 193]
    [1.0, 5.0, 14]
    [3.0, 5.0, 9]
    [4.0, 5.0, 147]
    [0.0, 6.0, 15]
    [1.0, 6.0, 18]
    [2.0, 6.0, 18]
    [3.0, 6.0, 12]
    

#### BƯỚC 4 – PHÂN TÍCH DWELL TIME (người đứng bao lâu)


```python
cursor.execute("""
WITH grid AS (
    SELECT
        track_id,
        floor(centroid_x / (1280 / 10)) AS gx,
        floor(centroid_y / (720 / 10)) AS gy,
        capture_ts
    FROM silver_detections_v2
),
dwell AS (
    SELECT
        track_id,
        gx,
        gy,
        MIN(capture_ts) AS start_time,
        MAX(capture_ts) AS end_time,
        MAX(capture_ts) - MIN(capture_ts) AS duration
    FROM grid
    GROUP BY track_id, gx, gy
)
SELECT *
FROM dwell
ORDER BY duration DESC
LIMIT 20
""")
print(cursor.fetchall())

```

    [[4, 7.0, 1.0, datetime.datetime(2025, 11, 21, 19, 48, 50, 907000), datetime.datetime(2025, 11, 21, 19, 49, 25, 843000), datetime.timedelta(seconds=34, microseconds=936000)], [3, 0.0, 5.0, datetime.datetime(2025, 11, 21, 19, 48, 50, 907000), datetime.datetime(2025, 11, 21, 19, 49, 24, 99000), datetime.timedelta(seconds=33, microseconds=192000)], [3, 0.0, 4.0, datetime.datetime(2025, 11, 21, 19, 48, 52, 814000), datetime.datetime(2025, 11, 21, 19, 49, 21, 935000), datetime.timedelta(seconds=29, microseconds=121000)], [7, 0.0, 2.0, datetime.datetime(2025, 11, 21, 19, 48, 59, 330000), datetime.datetime(2025, 11, 21, 19, 49, 21, 241000), datetime.timedelta(seconds=21, microseconds=911000)], [1, 4.0, 5.0, datetime.datetime(2025, 11, 21, 19, 48, 50, 907000), datetime.datetime(2025, 11, 21, 19, 49, 9, 800000), datetime.timedelta(seconds=18, microseconds=893000)], [6, 2.0, 2.0, datetime.datetime(2025, 11, 21, 19, 49, 3, 410000), datetime.datetime(2025, 11, 21, 19, 49, 17, 575000), datetime.timedelta(seconds=14, microseconds=165000)], [7, 0.0, 3.0, datetime.datetime(2025, 11, 21, 19, 49, 14, 248000), datetime.datetime(2025, 11, 21, 19, 49, 25, 843000), datetime.timedelta(seconds=11, microseconds=595000)], [7, 1.0, 2.0, datetime.datetime(2025, 11, 21, 19, 49, 2, 753000), datetime.datetime(2025, 11, 21, 19, 49, 10, 800000), datetime.timedelta(seconds=8, microseconds=47000)], [6, 3.0, 2.0, datetime.datetime(2025, 11, 21, 19, 48, 56, 842000), datetime.datetime(2025, 11, 21, 19, 49, 3, 270000), datetime.timedelta(seconds=6, microseconds=428000)], [1, 1.0, 4.0, datetime.datetime(2025, 11, 21, 19, 49, 21, 241000), datetime.datetime(2025, 11, 21, 19, 49, 25, 843000), datetime.timedelta(seconds=4, microseconds=602000)], [6, 1.0, 2.0, datetime.datetime(2025, 11, 21, 19, 49, 21, 778000), datetime.datetime(2025, 11, 21, 19, 49, 25, 843000), datetime.timedelta(seconds=4, microseconds=65000)], [1, 1.0, 5.0, datetime.datetime(2025, 11, 21, 19, 49, 17, 301000), datetime.datetime(2025, 11, 21, 19, 49, 21, 190000), datetime.timedelta(seconds=3, microseconds=889000)], [6, 1.0, 3.0, datetime.datetime(2025, 11, 21, 19, 49, 18, 43000), datetime.datetime(2025, 11, 21, 19, 49, 21, 731000), datetime.timedelta(seconds=3, microseconds=688000)], [7, 1.0, 3.0, datetime.datetime(2025, 11, 21, 19, 49, 10, 949000), datetime.datetime(2025, 11, 21, 19, 49, 14, 71000), datetime.timedelta(seconds=3, microseconds=122000)], [1, 0.0, 5.0, datetime.datetime(2025, 11, 21, 19, 49, 17, 511000), datetime.datetime(2025, 11, 21, 19, 49, 20, 541000), datetime.timedelta(seconds=3, microseconds=30000)], [2, 5.0, 2.0, datetime.datetime(2025, 11, 21, 19, 48, 50, 907000), datetime.datetime(2025, 11, 21, 19, 48, 53, 283000), datetime.timedelta(seconds=2, microseconds=376000)], [1, 2.0, 6.0, datetime.datetime(2025, 11, 21, 19, 49, 12, 801000), datetime.datetime(2025, 11, 21, 19, 49, 15, 109000), datetime.timedelta(seconds=2, microseconds=308000)], [1, 1.0, 6.0, datetime.datetime(2025, 11, 21, 19, 49, 15, 223000), datetime.datetime(2025, 11, 21, 19, 49, 17, 248000), datetime.timedelta(seconds=2, microseconds=25000)], [1, 3.0, 6.0, datetime.datetime(2025, 11, 21, 19, 49, 11, 182000), datetime.datetime(2025, 11, 21, 19, 49, 12, 678000), datetime.timedelta(seconds=1, microseconds=496000)], [2, 5.0, 3.0, datetime.datetime(2025, 11, 21, 19, 48, 53, 439000), datetime.datetime(2025, 11, 21, 19, 48, 54, 649000), datetime.timedelta(seconds=1, microseconds=210000)]]
    

#### BƯỚC 5 – PATTERN THEO THỜI GIAN (Temporal Analytics)

Trong bước này ta phân tích:

số người mỗi giây

số người mỗi frame

peak time

flow direction (trend di chuyển theo thời gian)

timeline của từng track

##### 5.1 – Số người theo giây


```python
cursor.execute("""
SELECT
    date_trunc('second', capture_ts) AS sec,
    COUNT(*) AS detections,
    COUNT(DISTINCT track_id) AS unique_people
FROM silver_detections_v2
GROUP BY 1
ORDER BY 1
""")
print(cursor.fetchall())

```

    [[datetime.datetime(2025, 11, 21, 19, 48, 50), 4, 4], [datetime.datetime(2025, 11, 21, 19, 48, 51), 28, 4], [datetime.datetime(2025, 11, 21, 19, 48, 52), 24, 4], [datetime.datetime(2025, 11, 21, 19, 48, 53), 24, 4], [datetime.datetime(2025, 11, 21, 19, 48, 54), 48, 5], [datetime.datetime(2025, 11, 21, 19, 48, 55), 64, 5], [datetime.datetime(2025, 11, 21, 19, 48, 56), 48, 4], [datetime.datetime(2025, 11, 21, 19, 48, 57), 20, 4], [datetime.datetime(2025, 11, 21, 19, 48, 58), 20, 4], [datetime.datetime(2025, 11, 21, 19, 48, 59), 28, 5], [datetime.datetime(2025, 11, 21, 19, 49), 35, 5], [datetime.datetime(2025, 11, 21, 19, 49, 1), 40, 5], [datetime.datetime(2025, 11, 21, 19, 49, 2), 35, 5], [datetime.datetime(2025, 11, 21, 19, 49, 3), 30, 5], [datetime.datetime(2025, 11, 21, 19, 49, 4), 40, 5], [datetime.datetime(2025, 11, 21, 19, 49, 5), 40, 5], [datetime.datetime(2025, 11, 21, 19, 49, 6), 40, 5], [datetime.datetime(2025, 11, 21, 19, 49, 7), 35, 5], [datetime.datetime(2025, 11, 21, 19, 49, 8), 40, 5], [datetime.datetime(2025, 11, 21, 19, 49, 9), 30, 5], [datetime.datetime(2025, 11, 21, 19, 49, 10), 35, 5], [datetime.datetime(2025, 11, 21, 19, 49, 11), 35, 5], [datetime.datetime(2025, 11, 21, 19, 49, 12), 40, 5], [datetime.datetime(2025, 11, 21, 19, 49, 13), 35, 5], [datetime.datetime(2025, 11, 21, 19, 49, 14), 40, 5], [datetime.datetime(2025, 11, 21, 19, 49, 15), 40, 5], [datetime.datetime(2025, 11, 21, 19, 49, 16), 35, 5], [datetime.datetime(2025, 11, 21, 19, 49, 17), 85, 5], [datetime.datetime(2025, 11, 21, 19, 49, 18), 80, 5], [datetime.datetime(2025, 11, 21, 19, 49, 19), 53, 5], [datetime.datetime(2025, 11, 21, 19, 49, 20), 42, 5], [datetime.datetime(2025, 11, 21, 19, 49, 21), 80, 5], [datetime.datetime(2025, 11, 21, 19, 49, 22), 70, 5], [datetime.datetime(2025, 11, 21, 19, 49, 23), 45, 5], [datetime.datetime(2025, 11, 21, 19, 49, 24), 55, 5], [datetime.datetime(2025, 11, 21, 19, 49, 25), 56, 6]]
    

##### 5.2 – Hướng di chuyển theo thời gian (x movement)


```python
cursor.execute("""
SELECT
    track_id,
    MIN(centroid_x) AS start_x,
    MAX(centroid_x) AS end_x,
    MAX(centroid_x) - MIN(centroid_x) AS movement_x
FROM silver_detections_v2
GROUP BY track_id
ORDER BY track_id
""")
print(cursor.fetchall())

```

    [[1, 108.0, 597.0, 489.0], [2, 682.0, 764.0, 82.0], [3, 27.0, 47.0, 20.0], [4, 956.0, 990.0, 34.0], [5, 105.0, 113.0, 8.0], [6, 132.0, 636.0, 504.0], [7, 91.0, 168.0, 77.0], [8, 170.0, 170.0, 0.0]]
    

##### 5.3 – Timeline của từng track


```python
cursor.execute("""
SELECT
    track_id,
    MIN(capture_ts),
    MAX(capture_ts),
    MAX(capture_ts) - MIN(capture_ts) AS duration
FROM silver_detections_v2
GROUP BY track_id
ORDER BY track_id
""")
print(cursor.fetchall())

```

    [[1, datetime.datetime(2025, 11, 21, 19, 48, 50, 907000), datetime.datetime(2025, 11, 21, 19, 49, 25, 843000), datetime.timedelta(seconds=34, microseconds=936000)], [2, datetime.datetime(2025, 11, 21, 19, 48, 50, 907000), datetime.datetime(2025, 11, 21, 19, 48, 54, 649000), datetime.timedelta(seconds=3, microseconds=742000)], [3, datetime.datetime(2025, 11, 21, 19, 48, 50, 907000), datetime.datetime(2025, 11, 21, 19, 49, 25, 301000), datetime.timedelta(seconds=34, microseconds=394000)], [4, datetime.datetime(2025, 11, 21, 19, 48, 50, 907000), datetime.datetime(2025, 11, 21, 19, 49, 25, 843000), datetime.timedelta(seconds=34, microseconds=936000)], [5, datetime.datetime(2025, 11, 21, 19, 48, 54, 695000), datetime.datetime(2025, 11, 21, 19, 48, 55, 527000), datetime.timedelta(microseconds=832000)], [6, datetime.datetime(2025, 11, 21, 19, 48, 55, 346000), datetime.datetime(2025, 11, 21, 19, 49, 25, 843000), datetime.timedelta(seconds=30, microseconds=497000)], [7, datetime.datetime(2025, 11, 21, 19, 48, 59, 330000), datetime.datetime(2025, 11, 21, 19, 49, 25, 843000), datetime.timedelta(seconds=26, microseconds=513000)], [8, datetime.datetime(2025, 11, 21, 19, 49, 25, 778000), datetime.datetime(2025, 11, 21, 19, 49, 25, 843000), datetime.timedelta(microseconds=65000)]]
    

### Lớp Gold

#### Bảng Gold 1: gold_people_per_minute
Ý nghĩa:
Mỗi dòng = 1 phút / 1 camera / 1 store:

có bao nhiêu detection (số bounding box)

có bao nhiêu người unique (track_id)


```python
cursor.execute("""
CREATE TABLE IF NOT EXISTS gold_people_per_minute (
    store_id varchar,
    camera_id varchar,
    ts_minute timestamp(6),
    detections bigint,
    unique_people bigint
)
WITH (format = 'PARQUET')
""")
print("Created gold_people_per_minute")

```

    Created gold_people_per_minute
    


```python
cursor.execute("""
INSERT INTO gold_people_per_minute
SELECT
    store_id,
    camera_id,
    date_trunc('minute', capture_ts) AS ts_minute,
    COUNT(*) AS detections,
    COUNT(DISTINCT track_id) AS unique_people
FROM silver_detections_v2
GROUP BY store_id, camera_id, date_trunc('minute', capture_ts)
""")
print("Inserted gold_people_per_minute")

```

    Inserted gold_people_per_minute
    

#### Bảng Gold 2: gold_zone_heatmap
Ý nghĩa:
Heatmap tổng hợp: mỗi dòng = 1 zone (ô lưới) trong khung hình + số lần xuất hiện:

dùng cho heatmap overlay

cluster xem khu vực nào đông khách

Ta tiếp tục dùng lưới 10×10 (như lúc nãy):

zone_x = 0..9 (trái → phải)

zone_y = 0..9 (trên → dưới)


```python
cursor.execute("""
CREATE TABLE IF NOT EXISTS gold_zone_heatmap (
    store_id varchar,
    camera_id varchar,
    zone_x integer,
    zone_y integer,
    hits bigint,
    unique_tracks bigint
)
WITH (format = 'PARQUET')
""")
print("Created gold_zone_heatmap")

```

    Created gold_zone_heatmap
    


```python
cursor.execute("""
INSERT INTO gold_zone_heatmap
SELECT
    store_id,
    camera_id,
    CAST(floor(centroid_x / (1280 / 10)) AS integer) AS zone_x,
    CAST(floor(centroid_y / (720 / 10)) AS integer) AS zone_y,
    COUNT(*) AS hits,
    COUNT(DISTINCT track_id) AS unique_tracks
FROM silver_detections_v2
GROUP BY
    store_id,
    camera_id,
    CAST(floor(centroid_x / (1280 / 10)) AS integer),
    CAST(floor(centroid_y / (720 / 10)) AS integer)
""")
print("Inserted gold_zone_heatmap")

```

    Inserted gold_zone_heatmap
    

#### Bảng Gold 3: gold_zone_dwell

Ý nghĩa:
Đây là bảng rất “xịn”:

mỗi dòng = 1 store + camera + zone

chứa thông tin thời gian khách đứng lại (dwell)

Từ Silver mình đã tính dwell theo track_id + zone_x + zone_y.
Giờ Gold sẽ tổng hợp lại:

tổng thời gian đứng (sum_dwell_seconds)

dwell trung bình (avg_dwell_seconds)

số lượt (số track) vào zone (visits)


```python
cursor.execute("""
CREATE TABLE IF NOT EXISTS gold_zone_dwell (
    store_id varchar,
    camera_id varchar,
    zone_x integer,
    zone_y integer,
    visits bigint,
    total_dwell_seconds double,
    avg_dwell_seconds double
)
WITH (format = 'PARQUET')
""")
print("Created gold_zone_dwell")

```

    Created gold_zone_dwell
    


```python
cursor.execute("""
INSERT INTO gold_zone_dwell
WITH per_track_zone AS (
    SELECT
        store_id,
        camera_id,
        track_id,
        CAST(floor(centroid_x / (1280 / 10)) AS integer) AS zone_x,
        CAST(floor(centroid_y / (720 / 10)) AS integer) AS zone_y,
        MIN(capture_ts) AS start_time,
        MAX(capture_ts) AS end_time,
        date_diff('millisecond', MIN(capture_ts), MAX(capture_ts)) / 1000.0 AS dwell_seconds
    FROM silver_detections_v2
    GROUP BY
        store_id,
        camera_id,
        track_id,
        CAST(floor(centroid_x / (1280 / 10)) AS integer),
        CAST(floor(centroid_y / (720 / 10)) AS integer)
)
SELECT
    store_id,
    camera_id,
    zone_x,
    zone_y,
    COUNT(*) AS visits,
    SUM(dwell_seconds) AS total_dwell_seconds,
    AVG(dwell_seconds) AS avg_dwell_seconds
FROM per_track_zone
GROUP BY store_id, camera_id, zone_x, zone_y
""")
print("Inserted gold_zone_dwell")

```

    Inserted gold_zone_dwell
    

#### Thiết kế bảng gold_track_summary
Schema đề xuất:

store_id – cửa hàng

camera_id – camera

track_id – id của người/track

frames – số frame xuất hiện

start_time, end_time – thời gian bắt đầu/kết thúc

duration_seconds – thời gian tồn tại của track

min_x, max_x, delta_x – biên độ di chuyển theo chiều ngang

min_y, max_y, delta_y – biên độ di chuyển theo chiều dọc

avg_x, avg_y – vị trí trung bình (điểm trung tâm “quen thuộc”)

avg_conf – confidence trung bình của YOLO cho track đó


```python
cursor.execute("""
CREATE TABLE IF NOT EXISTS gold_track_summary (
    store_id varchar,
    camera_id varchar,
    track_id bigint,
    frames bigint,
    start_time timestamp(6),
    end_time timestamp(6),
    duration_seconds double,
    min_x double,
    max_x double,
    delta_x double,
    min_y double,
    max_y double,
    delta_y double,
    avg_x double,
    avg_y double,
    avg_conf double
)
WITH (format = 'PARQUET')
""")
print("Created gold_track_summary")

```

    Created gold_track_summary
    


```python
cursor.execute("""
INSERT INTO gold_track_summary
SELECT
    store_id,
    camera_id,
    CAST(track_id AS bigint) AS track_id,
    COUNT(*) AS frames,
    MIN(capture_ts) AS start_time,
    MAX(capture_ts) AS end_time,
    date_diff('millisecond', MIN(capture_ts), MAX(capture_ts)) / 1000.0 AS duration_seconds,
    MIN(centroid_x) AS min_x,
    MAX(centroid_x) AS max_x,
    MAX(centroid_x) - MIN(centroid_x) AS delta_x,
    MIN(centroid_y) AS min_y,
    MAX(centroid_y) AS max_y,
    MAX(centroid_y) - MIN(centroid_y) AS delta_y,
    AVG(centroid_x) AS avg_x,
    AVG(centroid_y) AS avg_y,
    AVG(conf) AS avg_conf
FROM silver_detections_v2
GROUP BY
    store_id,
    camera_id,
    CAST(track_id AS bigint)
""")
print("Inserted gold_track_summary")

```

    Inserted gold_track_summary
    

##### query kiểm tra nhanh 


```python
# Top track đứng lâu nhất
cursor.execute("""
SELECT track_id, duration_seconds
FROM gold_track_summary
ORDER BY duration_seconds DESC
LIMIT 10
""")
print(cursor.fetchall())

# Track di chuyển xa nhất theo trục X
cursor.execute("""
SELECT track_id, delta_x, delta_y
FROM gold_track_summary
ORDER BY delta_x DESC
LIMIT 10
""")
print(cursor.fetchall())

```

    [[4, 34.9], [1, 34.9], [3, 34.4], [6, 30.5], [7, 26.5], [2, 3.7], [5, 0.8], [8, 0.1]]
    [[6, 504.0, 97.0], [1, 489.0, 130.0], [2, 82.0, 28.0], [7, 77.0, 105.0], [4, 34.0, 3.0], [3, 20.0, 125.0], [5, 8.0, 15.0], [8, 0.0, 1.0]]
    
