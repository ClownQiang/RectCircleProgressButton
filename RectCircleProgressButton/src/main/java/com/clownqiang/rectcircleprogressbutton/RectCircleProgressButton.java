package com.clownqiang.rectcircleprogressbutton;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by clownqiang on 16/7/19.
 */
public class RectCircleProgressButton extends View
		implements
			View.OnClickListener {

	public static final int RECTANGLE = 0;
	public static final int REC_TO_CIRCLE = 1;
	public static final int CIRCLE_TO_REC = 2;
	public static final int CIRCLE = 3;
	public static final int CIRCLE_ROUND = 4;

	private static final String DEFAULT_TEXT = "Download";

	private AnimationStatusListener listener;
	private int status = RECTANGLE;

	private Paint paint;
	private int recColor = Color.BLACK;
	private int recToCircleColor = Color.BLACK;
	private int circleColor = Color.RED;
	// 圆角半径
	private int r = 0;
	// 圆形的圆心坐标
	private int x, y = 0;
	// 画笔宽度
	private int strokeWidth = 0;
	// Button长宽
	private int REC_WIDTH;
	private int REC_HEIGHT;
	private int LIMIT_VALUE;
	private int drawWidth;
	private int drawHeight;
	// 由长方形变化到圆形动画时间
	private int recToCircleInterval;
	// 旋转动画速度(ms)
	private int rotateSpeed;
	// 默认显示的文字
	private String recText;
	private int recTextColor;
	private int recTextSize;
	// 旋转角度变化
	private int startAngle = 15;
	// 控制CIRCLE_ROUND 在线程中绘制
	private boolean isCircleRoundRun = false;

	private boolean lock = false;

	private CircleRoundThread circleRoundTask;

	private ExecutorService executors;

	private MessageHandler handler;

	public RectCircleProgressButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	private void init(Context context, AttributeSet attrs) {
		TypedArray typedArray = null;
		try {
			typedArray = context.obtainStyledAttributes(attrs,
					R.styleable.RectCircleProgressButton);
			recText = typedArray
					.getString(R.styleable.RectCircleProgressButton_rcpb_rec_text);
			recTextColor = typedArray.getColor(
					R.styleable.RectCircleProgressButton_rcpb_rec_text_color,
					Color.BLACK);
			recTextSize = typedArray.getDimensionPixelSize(
					R.styleable.RectCircleProgressButton_rcpb_rec_text_size,
					getResources().getDimensionPixelSize(
							R.dimen.font_text_size_default));
			recColor = typedArray.getColor(
					R.styleable.RectCircleProgressButton_rcpb_rec_color,
					Color.BLACK);
			recToCircleColor = typedArray
					.getColor(
							R.styleable.RectCircleProgressButton_rcpb_rec_to_circle_color,
							Color.BLACK);
			circleColor = typedArray.getColor(
					R.styleable.RectCircleProgressButton_rcpb_circle_color,
					Color.RED);
			strokeWidth = typedArray.getDimensionPixelSize(
					R.styleable.RectCircleProgressButton_rcpb_rec_stoke_width,
					2);
			rotateSpeed = typedArray.getInteger(
					R.styleable.RectCircleProgressButton_rcpb_rotate_speed, 20);
			recToCircleInterval = typedArray
					.getInteger(
							R.styleable.RectCircleProgressButton_rcpb_rec_to_circle_interval,
							800);
			recText = null == recText ? DEFAULT_TEXT : recText;
		} finally {
			if (typedArray != null) {
				typedArray.recycle();
			}
		}
		initPaint();
		initThread();
		setOnClickListener(this);
	}
	private void initPaint() {
		paint = new Paint();
		paint.setAntiAlias(true);
	}

	private void initThread() {
		handler = new MessageHandler(this);
		executors = Executors.newSingleThreadExecutor();
		circleRoundTask = new CircleRoundThread();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		initRectValue();
	}

	private void initRectValue() {
		REC_WIDTH = getMeasuredWidth();
		REC_HEIGHT = getMeasuredHeight();
		LIMIT_VALUE = (REC_WIDTH + REC_HEIGHT) / 2;

		drawWidth = REC_WIDTH;
		drawHeight = REC_HEIGHT;

		r = REC_HEIGHT / 2;
		x = REC_WIDTH / 2;
		y = REC_HEIGHT / 2;

	}
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		drawTypeByStatus(status, canvas, drawWidth, drawHeight);
	}

	private void drawTypeByStatus(int status, Canvas canvas, int width,
			int height) {
		switch (status) {
			case RECTANGLE :
				paint.setColor(recColor);
				paint.setStrokeWidth(strokeWidth);
				paint.setStyle(Paint.Style.STROKE);
				drawRoundRect(width, height, canvas);

				paint.setTextAlign(Paint.Align.CENTER);
				paint.setColor(recTextColor);
				paint.setTextSize(recTextSize);
				paint.setStyle(Paint.Style.FILL);
				drawText(width, height, recText, canvas);
				break;
			case REC_TO_CIRCLE :
				paint.setColor(recToCircleColor);
				paint.setStyle(Paint.Style.STROKE);
				paint.setStrokeWidth(strokeWidth);
				drawRoundRect(width, height, canvas);
				break;
			case CIRCLE_TO_REC :
				paint.setColor(recToCircleColor);
				paint.setStyle(Paint.Style.STROKE);
				paint.setStrokeWidth(strokeWidth);
				drawRoundRect(width, height, canvas);
				break;
			case CIRCLE :
				paint.setColor(circleColor);
				paint.setStyle(Paint.Style.STROKE);
				paint.setStrokeWidth(strokeWidth);
				drawCircle(canvas);
				drawX(canvas);
				break;
			case CIRCLE_ROUND :
				paint.setColor(circleColor);
				paint.setStyle(Paint.Style.STROKE);
				paint.setStrokeWidth(strokeWidth);

				drawCircleRound(canvas);
				drawX(canvas);
				break;
		}
	}

	private void drawRoundRect(int width, int height, Canvas canvas) {
		RectF roundRect = getRoundRectF(width, height);
		canvas.drawRoundRect(roundRect, r, r, paint);
	}

	private void drawCircle(Canvas canvas) {
		canvas.drawCircle(x, y, r - strokeWidth, paint);
	}

	private void drawText(int width, int height, String recText, Canvas canvas) {
		RectF rectF = getRoundRectF(width, height);
		Paint.FontMetricsInt fontMetricsInt = paint.getFontMetricsInt();
		int baseLine = (int) ((rectF.bottom + rectF.top - fontMetricsInt.bottom - fontMetricsInt.top) / 2);
		canvas.drawText(recText, rectF.centerX(), baseLine, paint);
	}

	private void drawX(Canvas canvas) {
		Path path = new Path();
		int r = this.r / 3;
		path.moveTo(x - r, y + r);
		path.lineTo(x + r, y - r);
		path.moveTo(x - r, y - r);
		path.lineTo(x + r, y + r);
		canvas.drawPath(path, paint);
	}

	private void drawCircleRound(Canvas canvas) {
		int r = this.r - strokeWidth;
		Path path = new Path();
		path.moveTo(x - r, y - r);
		RectF oval = new RectF(x - r, y - r, x + r, y + r);
		path.arcTo(oval, startAngle, 345, true);

		canvas.drawPath(path, paint);
	}

	private RectF getRoundRectF(int width, int height) {
		return new RectF(REC_WIDTH - width + strokeWidth, strokeWidth, width
				- strokeWidth, height - strokeWidth);
	}

	private void setStatusToCircleRound() {
		// 一次view转换的终态
		status = CIRCLE_ROUND;
		isCircleRoundRun = true;
		executors.submit(circleRoundTask);
	}

	private void reset() {
		startAngle = 15;
		lock = false;
		isCircleRoundRun = false;
	}

	@Override
	public void onClick(View view) {
		if (CIRCLE_ROUND == status) {
			reset();
			status = CIRCLE;
		}
		if (!lock) {
			lock = true;
			rectToCirWidthChange();
		}
	}

	/**
	 * 如果在加载过程由于网络问题，调用此方法重置View状态
	 */
	public void resetButtonView() {
		if (CIRCLE_ROUND == status) {
			reset();
			status = CIRCLE;
		}
		if (!lock && status == CIRCLE) {
			lock = true;
			rectToCirWidthChange();
		}
	}

	public interface AnimationStatusListener {
		void startLoading(int status);
		void resetView(int status);
	}

	public void setAnimationRectButtonListener(AnimationStatusListener listener) {
		this.listener = listener;
	}

	private void handleResetView() {
		if (null != listener) {
			listener.resetView(status);
		}
	}

	private void handleStartLoading() {
		if (null != listener) {
			listener.startLoading(status);
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void rectToCirWidthChange() {
		ValueAnimator valueAnimator = null;
		if (status == RECTANGLE) {
			status = REC_TO_CIRCLE;
			valueAnimator = ValueAnimator.ofInt(REC_WIDTH, LIMIT_VALUE);
			valueAnimator.addListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					super.onAnimationEnd(animation);
					status = CIRCLE;
					handleStartLoading();
					handleMessage();
					setStatusToCircleRound();
				}
			});
		} else if (status == CIRCLE) {
			status = CIRCLE_TO_REC;
			valueAnimator = ValueAnimator.ofInt(LIMIT_VALUE, REC_WIDTH);
			valueAnimator.addListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					super.onAnimationEnd(animation);
					status = RECTANGLE;
					handleResetView();
					handleMessage();
				}
			});
		}

		assert valueAnimator != null;
		valueAnimator
				.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
					@Override
					public void onAnimationUpdate(ValueAnimator valueAnimator) {
						drawWidth = (int) valueAnimator.getAnimatedValue();
						handleMessage();
					}

				});
		valueAnimator.setDuration(recToCircleInterval);
		valueAnimator.start();
	}

	class CircleRoundThread implements Runnable {

		@Override
		public void run() {
			while (isCircleRoundRun) {
				sleep(rotateSpeed);
				handleMessage();
			}
		}
	}

	private static class MessageHandler extends Handler {
		private final WeakReference<RectCircleProgressButton> button;

		public MessageHandler(RectCircleProgressButton rectCircleProgressButton) {
			this.button = new WeakReference<RectCircleProgressButton>(
					rectCircleProgressButton);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			RectCircleProgressButton button = this.button.get();
			if (button != null) {
				switch (msg.what) {
					case RECTANGLE :
						// 重置一下状态
						button.drawWidth = button.REC_WIDTH;
						button.reset();
						break;
					case REC_TO_CIRCLE :
						break;
					case CIRCLE_TO_REC :
						if (button.drawWidth > button.REC_WIDTH) {
							button.drawWidth = button.REC_WIDTH;
						}
						break;
					case CIRCLE :
						button.lock = false;
						break;
					case CIRCLE_ROUND :
						button.startAngle = button.startAngle + 10;
						break;
				}
				button.invalidate();
			}
		}
	}

	private void handleMessage() {
		Message message = new Message();
		message.what = status;
		handler.sendMessage(message);
	}

	private void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
