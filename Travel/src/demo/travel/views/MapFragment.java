package demo.travel.views;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.map.ItemizedOverlay;
import com.baidu.mapapi.map.LocationData;
import com.baidu.mapapi.map.MKMapStatus;
import com.baidu.mapapi.map.MKMapStatusChangeListener;
import com.baidu.mapapi.map.MKMapViewListener;
import com.baidu.mapapi.map.MapController;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationOverlay;
import com.baidu.mapapi.map.OverlayItem;
import com.baidu.platform.comapi.basestruct.GeoPoint;
import com.loopj.android.http.AsyncHttpResponseHandler;

import demo.travel.R;
import demo.travel.net.HotelDao;

public class MapFragment extends Fragment {
	private String TAG = "HOTEL_MAIN";
	private LocationData locData = null;
	private ItemizedOverlay myMarkerOverLay;
	private MyLocationOverlay myLocationOverlay;
	private GeoPoint targetGeo = null;
	private Boolean isFirstLoc = true;
	private HotelDao hotelDao = null;

	private View mRootView = null;

	// 百度地图SDK
	private BMapManager mBMapMan = null;
	private MapView mMapView = null;
	private MapController mMapController = null;

	// 百度定位SDK
	private LocationClient mLocClient = null;

	private static final int UPDATE_TIME = 1000;

	private void initMap() {
		this.mMapView = (MapView) this.mRootView.findViewById(R.id.bmapsView);
		this.mMapView.setBuiltInZoomControls(false);
		this.mMapController = this.mMapView.getController();
		this.mMapController.setZoom(14.0F);
		this.mMapController.enableClick(false);
		this.mMapController.setRotationGesturesEnabled(false);
		this.mMapController.setOverlookingGesturesEnabled(false);
		this.mMapController.setRotateWithTouchEventCenterEnabled(false);
		this.mMapController.setZoomWithTouchEventCenterEnabled(false);
		// enableMapViewControl();
		this.mLocClient = new LocationClient(getActivity()
				.getApplicationContext());

		// 设置定位条件
		LocationClientOption option = new LocationClientOption();
		option.setOpenGps(true); // 是否打开GPS
		option.setCoorType("bd09ll"); // 设置返回值的坐标类型。
		// option.setCoorType("gcj02"); // 设置返回值的坐标类型。
		option.setPriority(LocationClientOption.NetWorkFirst); // 设置定位优先级
		// option.setProdName("LocationDemo"); //
		// 设置产品线名称。强烈建议您使用自定义的产品线名称，方便我们以后为您提供更高效准确的定位服务。
		option.setScanSpan(UPDATE_TIME); // 设置定时定位的时间间隔。单位毫秒
		mLocClient.setLocOption(option);

		this.locData = new LocationData();
		this.mLocClient.registerLocationListener(new MyLocationListener());
		this.mLocClient.start();

		this.myLocationOverlay = new MyLocationOverlay(this.mMapView);
		this.myLocationOverlay
				.setLocationMode(MyLocationOverlay.LocationMode.NORMAL);
		this.myLocationOverlay.setData(this.locData);
		this.mMapView.getOverlays().add(this.myLocationOverlay);

		this.myMarkerOverLay = new ItemizedOverlay(getResources().getDrawable(
				R.drawable.marker), mMapView);
		this.mMapView.getOverlays().add(this.myMarkerOverLay);
		this.mMapView.refresh();
	}

	private void initUI() {
		OnClickListener clickListener = new ClickListener();
		
		ImageButton locationButton = ((ImageButton) mRootView
				.findViewById(R.id.location_button));
		locationButton.setOnClickListener(clickListener);
		ImageButton zoomInButton = ((ImageButton) mRootView
				.findViewById(R.id.zoom_in));
		zoomInButton.setOnClickListener(clickListener);
		ImageButton zoomOutButton = ((ImageButton) mRootView
				.findViewById(R.id.zoom_out));
		zoomOutButton.setOnClickListener(clickListener);
	}

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
		View localView = getActivity().getLayoutInflater().inflate(
				R.layout.marker, null);
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
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mBMapMan = new BMapManager(getActivity().getApplication());
		mBMapMan.init("y5qvr6Apv4vC3t9SzlteXARS", null);

		mRootView = inflater.inflate(R.layout.fragment_map, container, false);

		this.initMap();
		this.initUI();
		
		hotelDao = new HotelDao();

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
				searchTargetAroundHotel();
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
				targetGeo = mapStatus.targetGeo; // 中心点的地理坐标
			}
		};
		mMapView.regMapStatusChangeListener(listener); // 注册监听

		return mRootView;
	}

	@Override
	public void onDestroy() {
		mMapView.destroy();
		if (mBMapMan != null) {
			mBMapMan.destroy();
			mBMapMan = null;
		}
		super.onDestroy();

		if (mLocClient != null && mLocClient.isStarted()) {
			mLocClient.stop();
			mLocClient = null;
		}
	}

	@Override
	public void onPause() {
		mMapView.onPause();
		if (mBMapMan != null) {
			mBMapMan.stop();
		}
		super.onPause();
	}

	@Override
	public void onResume() {
		mMapView.onResume();
		if (mBMapMan != null) {
			mBMapMan.start();
		}
		super.onResume();
	}

	public class MyLocationListener implements BDLocationListener {

		public MyLocationListener() {
		}

		@Override
		public void onReceiveLocation(BDLocation location) {
			if (location == null) {
				return;
			}
			Double latitude = location.getLatitude();
			Double longtitude = location.getLongitude();

			MapFragment.this.locData.latitude = latitude;
			MapFragment.this.locData.longitude = longtitude;
			MapFragment.this.myLocationOverlay
					.setData(MapFragment.this.locData);
			MapFragment.this.mMapView.refresh();

			GeoPoint point = new GeoPoint((int) (latitude * 1E6),
					(int) (longtitude * 1E6));

			targetGeo = point;
			if (MapFragment.this.isFirstLoc.booleanValue()) {
				MapFragment.this.mMapController.setZoom(14.0F);
				MapFragment.this.mMapController.animateTo(point);
				searchTargetAroundHotel();
				MapFragment.this.isFirstLoc = Boolean.valueOf(false);
			}
		}

		@Override
		public void onReceivePoi(BDLocation location) {
		}
	}

	public class ClickListener implements OnClickListener {
		public void onClick(View paramView) {
			switch (paramView.getId()) {
			case R.id.zoom_in:
				MapFragment.this.mMapController.zoomIn();
				return;
			case R.id.zoom_out:
				MapFragment.this.mMapController.zoomOut();
				return;
			}
		}
	}
}