package demo.travel.net;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

public class HotelDao {
	//private static String AROUND_HOTEL = "http://115.28.129.120:8080/travel/services/around/hotels?lnt=%s&lat=%s";
	private static String AROUND_HOTEL = "http://115.28.129.120:8080/travel/services/around/hotels?lnt=%s&lat=%s";

	private AsyncHttpClient asyncHttpClient = new AsyncHttpClient();

	public void getAroundHotel(Double latitude, Double longtitude,
			AsyncHttpResponseHandler asyncHttpResponseHandler) {
		asyncHttpClient.get(
				String.format(AROUND_HOTEL, longtitude.toString(),
						latitude.toString()), asyncHttpResponseHandler);
	}
}
