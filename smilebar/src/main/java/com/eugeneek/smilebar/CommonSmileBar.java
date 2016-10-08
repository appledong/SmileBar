package com.eugeneek.smilebar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;


/**
 * Created by dongdz on 2016/08/10.
 */
public class CommonSmileBar extends View {

    private static final int NO_RATING = 0;
    private static final int MAX_RATE = 5;
    private boolean isSliding;
    private float slidePosition;
    private PointF[] points;
    private float itemWidth;
    private Drawable fullSmile;
    private Drawable halfSmile;
    private Drawable defaultSmile;
    private OnRatingSliderChangeListener listener;
    private float currentRating = NO_RATING;
    private int smileWidth, smileHeight;
    private int horizontalSpacing;
    private boolean isEnabled;
    private float rating = NO_RATING;
    private boolean isKeepOneSmile = false;

    public CommonSmileBar(Context context) {
        super(context);
        init();
    }

    public CommonSmileBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public CommonSmileBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    private void init() {
        init(null);
    }

    private void init(AttributeSet attrs) {
        isSliding = false;

        if (attrs != null) {
            TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.SmileBar, 0, 0);
            try {
                smileWidth = ta.getDimensionPixelSize(R.styleable.SmileBar_smileWidth, 0);
                smileHeight = ta.getDimensionPixelSize(R.styleable.SmileBar_smileHeight, 0);
                horizontalSpacing = ta.getDimensionPixelSize(R.styleable.SmileBar_horizontalSpacing, 0);
                isEnabled = ta.getBoolean(R.styleable.SmileBar_enabled, true);
                rating = ta.getFloat(R.styleable.SmileBar_commonrating, NO_RATING);
                int resDefault = ta.getResourceId(R.styleable.SmileBar_smileDefault, R.drawable.smile_default);
                defaultSmile = ResourcesCompat.getDrawable(getResources(), resDefault, null);
                int resFull = ta.getResourceId(R.styleable.SmileBar_smileFull, R.drawable.smile_full);
                fullSmile = ResourcesCompat.getDrawable(getResources(), resFull, null);
                int resHalf = ta.getResourceId(R.styleable.SmileBar_smileHalf, R.drawable.smile_half);
                halfSmile = ResourcesCompat.getDrawable(getResources(), resHalf, null);
                isKeepOneSmile = ta.getBoolean(R.styleable.SmileBar_isKeepOneSmile, false);
                if (smileWidth == 0)
                    smileWidth = defaultSmile.getIntrinsicWidth();
                if (smileHeight == 0)
                    smileHeight = defaultSmile.getIntrinsicHeight();
            } finally {
                ta.recycle();
            }
        }

        points = new PointF[MAX_RATE];
        for (int i = 0; i < MAX_RATE; i++) {
            points[i] = new PointF();
        }
        if (rating != NO_RATING)
            setRating(rating);
    }

    public void setKeepOneSmile(boolean keepOneSmile) {
        isKeepOneSmile = keepOneSmile;
    }

    @Override
    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
        super.setEnabled(enabled);
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * Set a listener that will be invoked whenever the users interacts with the SmileBar.
     *
     * @param listener Listener to set.
     */
    public void setOnRatingSliderChangeListener(OnRatingSliderChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            // Disable all input if the slider is disabled
            return false;
        }
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE: {
                isSliding = true;
                slidePosition = getRelativePosition(event.getX());
                rating = convertPositionToRating();
                if (listener != null && rating != currentRating) {
                    currentRating = rating;
                    listener.onPendingRating(rating);
                }
                break;
            }
            case MotionEvent.ACTION_UP:
                currentRating = NO_RATING;
                rating = convertPositionToRating();
                if (listener != null)
                    listener.onFinalRating(rating);
                break;
            case MotionEvent.ACTION_CANCEL:
                currentRating = NO_RATING;
                if (listener != null)
                    listener.onCancelRating();
                isSliding = false;
                break;
            default:
                break;
        }

        invalidate();
        return true;
    }

    private float convertPositionToRating() {
        if (slidePosition >= 0 && slidePosition <= 1) {
            if (!isKeepOneSmile && isHalfSmile()) {
                return 0.5f;
            } else {
                if (isKeepOneSmile) {
                    return 1.0f;
                }
                if (isHalfSmile()) {
                    return (float) Math.floor(slidePosition);
                } else {
                    return (float) Math.ceil(slidePosition);
                }
            }
        } else {
            if (isHalfSmile()) {
                return (float) (Math.floor(slidePosition) + 0.5f);
            } else {
                return (float) Math.ceil(slidePosition);
            }
        }
    }

    private boolean isHalfSmile() {
        float value = slidePosition % 1;
        if (value > 0.0f && value <= 0.5f) {
            return true;
        } else {
            return false;
        }
    }

    private float getRelativePosition(float x) {
        float position = x / itemWidth;
        position = Math.max(position, 0);
        return Math.min(position, MAX_RATE);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        itemWidth = w / (float) MAX_RATE;
        updatePositions();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int width = smileWidth * MAX_RATE + horizontalSpacing * (MAX_RATE - 1) +
                getPaddingLeft() + getPaddingRight();
        int height = smileHeight + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (int i = 0; i < MAX_RATE; i++) {
            PointF pos = points[i];
            canvas.save();
            canvas.translate(pos.x, pos.y);
            drawSmile(canvas, i);
            canvas.restore();
        }
    }

    private void drawSmile(Canvas canvas, int position) {
        if (isSliding && position <= slidePosition) {
            float rating = convertPositionToRating();
            if (rating > 0) {
                if (isKeepOneSmile && rating <= 1) {
                    drawSmile(canvas, fullSmile);
                    return;
                }
                if (slidePosition == 0.0f) {
                    drawSmile(canvas, defaultSmile);
                    return;
                }
                if (isHalfSmile()) {
                    if (position > rating - 1) {
                        drawSmile(canvas, halfSmile);
                    } else {
                        drawSmile(canvas, fullSmile);
                    }
                } else {
                    drawSmile(canvas, fullSmile);
                }
            } else if (rating == 0) {
                if (isKeepOneSmile) {
                    drawSmile(canvas, fullSmile);
                } else {
                    drawSmile(canvas, defaultSmile);
                }
            } else
                drawSmile(canvas, defaultSmile);
        } else {
            if (isKeepOneSmile && Float.compare(slidePosition, -0.1f) != 0 && position == 0) {
                drawSmile(canvas, fullSmile);
            } else {
                drawSmile(canvas, defaultSmile);
            }
        }
    }

    private void drawSmile(Canvas canvas, Drawable smile) {
        canvas.save();
        canvas.translate(-smileWidth / 2, -smileHeight / 2);
        smile.setBounds(0, 0, smileWidth, smileHeight);
        smile.draw(canvas);
        canvas.restore();
    }

    private void updatePositions() {
        float left = 0;
        for (int i = 0; i < MAX_RATE; i++) {
            float posY = getHeight() / 2;
            float posX = left + smileWidth / 2;
            left += smileWidth;
            if (i > 0) {
                posX += horizontalSpacing;
                left += horizontalSpacing;
            } else {
                posX += getPaddingLeft();
                left += getPaddingLeft();
            }

            points[i].set(posX, posY);

        }
    }

    public void setRating(float rating) {
        if (rating < 0 || rating > MAX_RATE)
            throw new IndexOutOfBoundsException("Rating must be between 0 and " + MAX_RATE);

        this.rating = rating;
        slidePosition = (float) (rating - 0.1);
        isSliding = true;
        invalidate();
        if (listener != null)
            listener.onFinalRating(rating);
    }

    public float getRating() {
        return rating;
    }

    /**
     * A callback that notifies clients when the user starts rating, changes the rating
     * value and when the rating has ended.
     */
    public interface OnRatingSliderChangeListener {

        /**
         * Notification that the user has moved over to a different rating value.
         * The rating value is only temporary and might change again before the
         * rating is finalized.
         *
         * @param rating the pending rating. A value between 0 and 5.
         */
        void onPendingRating(float rating);

        /**
         * Notification that the user has selected a final rating.
         *
         * @param rating the final rating selected. A value between 0 and 5.
         */
        void onFinalRating(float rating);

        /**
         * Notification that the user has canceled the rating.
         */
        void onCancelRating();
    }
}
