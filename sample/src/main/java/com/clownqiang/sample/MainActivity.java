package com.clownqiang.sample;

import com.clownqiang.rectcircleprogressbutton.RectCircleProgressButton;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity {

	private static final String TAG = "MainActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		final RectCircleProgressButton button = (RectCircleProgressButton) findViewById(R.id.btn_rect_circle);
		button.setAnimationRectButtonListener(new RectCircleProgressButton.AnimationStatusListener() {
			@Override
			public void startLoading(int status) {
				Log.d(TAG, "startLoading..." + status);
			}

			@Override
			public void resetView(int status) {
				Log.d(TAG, "resetView..." + status);
			}
		});

		findViewById(R.id.btn_reset).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						button.resetButtonView();
					}
				});
	}
}
