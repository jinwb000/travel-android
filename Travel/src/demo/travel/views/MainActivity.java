package demo.travel.views;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.map.ItemizedOverlay;
import com.baidu.mapapi.map.MKMapStatus;
import com.baidu.mapapi.map.MKMapStatusChangeListener;
import com.baidu.mapapi.map.MKMapViewListener;
import com.baidu.mapapi.map.MapController;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.OverlayItem;
import com.baidu.platform.comapi.basestruct.GeoPoint;
import com.loopj.android.http.AsyncHttpResponseHandler;

import demo.travel.R;
import demo.travel.net.HotelDao;

public class MainActivity extends ActionBarActivity {
	private String TAG = "HOTEL_MAIN";
	private ItemizedOverlay myMarkerOverLay;
	private GeoPoint targetGeo;
	private Boolean isFirstLoc = true;
	private HotelDao hotelDao = null;

	// 百度地图SDK
	private BMapManager mBMapMan = null;
	private MapView mMapView = null;
	private MapController mMapController = null;

	// 百度定位SDK
	private LocationClient locationClient = null;
	private static final int UPDATE_TIME = 5000;

	private void addItemToMyOverLay(double paramDouble1, double paramDouble2,
			String paramString, Object paramObject) {
		OverlayItem localOverlayItem = new OverlayItem(new GeoPoint(
				(int) (paramDouble1 * 1E6), (int) (paramDouble2 * 1E6)),
				paramString, "");
		localOverlayItem.setMarker(new BitmapDrawable(getResources(),
				getViewBitmap(getView(paramString))));
		this.myMarkerOverLay.addItem(localOverlayItem);
	}

	public static Bitmap getViewBitmap(View paramView) {
		paramView.measure(View.MeasureSpec.makeMeasureSpec(0, 0),
				View.MeasureSpec.makeMeasureSpec(0, 0));
		paramView.layout(0, 0, paramView.getMeasuredWidth(),
				paramView.getMeasuredHeight());
		paramView.buildDrawingCache();
		return paramView.getDrawingCache();
	}

	private View getView(String paramString) {
		View localView = getLayoutInflater().inflate(R.layout.marker, null);
		TextView localTextView = (TextView) localView
				.findViewById(R.id.hotel_brand);
		String str = paramString;
		if (paramString.length() >= 6)
			str = paramString.substring(0, 6);
		localTextView.setText(str);
		localTextView.setTextColor(-1);
		return localView;
	}

	private void searchTargetAroundHotel() {
		Double latitude = (double) targetGeo.getLatitudeE6() / 1E6;
		Double longtitude = (double) targetGeo.getLongitudeE6() / 1E6;
		searchAroundHotel(latitude, longtitude);
	}

	private void searchAroundHotel(Double latitude, Double longtitude) {
		hotelDao.getAroundHotel(latitude, longtitude,
				new AsyncHttpResponseHandler() {

					@Override
					public void onSuccess(String response) {
						myMarkerOverLay.removeAll();
						try {
							JSONArray ja = new JSONArray(response);
							for (int i = 0; i < ja.length() && i < 20; i++) {
								JSONObject jo = ja.getJSONObject(i);
								addItemToMyOverLay(jo.getDouble("latitude"),
										jo.getDouble("longtitude"),
										jo.getString("name"), null);
							}
							mMapView.refresh();
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}

					@Override
					public void onStart() {
						super.onStart();
						Log.d(TAG, "Start searchAroundHotel");
					}

					@Override
					public void onFinish() {
						super.onFinish();
						Log.d(TAG, "Finish searchAroundHotel");
					}

				});
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mBMapMan = new BMapManager(getApplication());
		mBMapMan.init("y5qvr6Apv4vC3t9SzlteXARS", null);

		// 注意：请在试用setContentView前初始化BMapManager对象，否则会报错
		setContentView(R.layout.activity_main);

		mMapView = (MapView) findViewById(R.id.bmapsView);

		// 设置启用内置的缩放控件
		mMapView.setBuiltInZoomControls(true);

		mMapController = mMapView.getController();

		hotelDao = new HotelDao();

		this.myMarkerOverLay = new ItemizedOverlay(getResources().getDrawable(
				R.drawable.marker), mMapView);
		mMapView.getOverlays().clear();
		mMapView.getOverlays().add(this.myMarkerOverLay);

		MKMapViewListener mapViewListener = new MKMapViewListener() {

			@Override
			public void onMapMoveFinish() {
				// 此处可以实现地图移动完成事件的状态监听
				searchTargetAroundHotel();
			}

			@Override
			public void onClickMapPoi(MapPoi arg0) {
				// 此处可实现点击到地图可点标注时的监听
			}

			@Override
			public void onGetCurrentMap(Bitmap b) {
				// 用MapView.getCurrentMap()发起截图后，在此处理截图结果.
			}

			@Override
			public void onMapAnimationFinish() {
				/**
				 * 地图完成带动画的操作（如: animationTo()）后，此回调被触发
				 */
			}

			@Override
			public void onMapLoadFinish() {
				// 地图初始化完成时，此回调被触发.
			}
		};
		mMapView.regMapViewListener(mBMapMan, mapViewListener); // 注册监听

		// 实现对地图状态改变的处理
		MKMapStatusChangeListener listener = new MKMapStatusChangeListener() {
			public void onMapStatusChange(MKMapStatus mapStatus) {
				float zoom = mapStatus.zoom; // 地图缩放等级
				int overlooking = mapStatus.overlooking; // 地图俯视角度
				int rotate = mapStatus.rotate; // 地图旋转角度
				targetGeo = mapStatus.targetGeo; // 中心点的地理坐标
				Point targetScreen = mapStatus.targetScreen; // 中心点的屏幕坐标
				// TODO add your process

			}
		};
		mMapView.regMapStatusChangeListener(listener); // 注册监听

		locationClient = new LocationClient(this);
		// 设置定位条件
		LocationClientOption option = new LocationClientOption();
		option.setOpenGps(true); // 是否打开GPS
		option.setCoorType("bd09ll"); // 设置返回值的坐标类型。
		// option.setCoorType("gcj02"); // 设置返回值的坐标类型。
		option.setPriority(LocationClientOption.NetWorkFirst); // 设置定位优先级
		option.setProdName("LocationDemo"); // 设置产品线名称。强烈建议您使用自定义的产品线名称，方便我们以后为您提供更高效准确的定位服务。
		option.setScanSpan(UPDATE_TIME); // 设置定时定位的时间间隔。单位毫秒
		locationClient.setLocOption(option);

		// 注册位置监听器
		locationClient.registerLocationListener(new BDLocationListener() {

			@Override
			public void onReceiveLocation(BDLocation location) {
				if (location == null) {
					return;
				}
				Double latitude = location.getLatitude();
				Double longtitude = location.getLongitude();

				mMapController = mMapView.getController();
				GeoPoint point = new GeoPoint((int) (latitude * 1E6),
						(int) (longtitude * 1E6));

				targetGeo = point;
				if (MainActivity.this.isFirstLoc.booleanValue()) {
					MainActivity.this.mMapController.setZoom(14.0F);
					MainActivity.this.mMapController.animateTo(point);
					searchTargetAroundHotel();
				}
				MainActivity.this.isFirstLoc = Boolean.valueOf(false);
			}

			@Override
			public void onReceivePoi(BDLocation location) {
			}
		});

		if (locationClient == null) {
			return;
		}
		if (locationClient.isStarted()) {
			locationClient.stop();
		} else {
			locationClient.start();
			locationClient.requestLocation();
		}
	}

	@Override
	protected void onDestroy() {
		mMapView.destroy();
		if (mBMapMan != null) {
			mBMapMan.destroy();
			mBMapMan = null;
		}
		super.onDestroy();

		if (locationClient != null && locationClient.isStarted()) {
			locationClient.stop();
			locationClient = null;
		}
	}

	@Override
	protected void onPause() {
		mMapView.onPause();
		if (mBMapMan != null) {
			mBMapMan.stop();
		}
		super.onPause();
	}

	@Override
	protected void onResume() {
		mMapView.onResume();
		if (mBMapMan != null) {
			mBMapMan.start();
		}
		super.onResume();
	}
}