/***************************************************************************
 *   Copyright 2005-2009 Last.fm Ltd.                                      *
 *   Portions contributed by Casey Link, Lukasz Wisniewski,                *
 *   Mike Jennings, and Michael Novak Jr.                                  *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.         *
 ***************************************************************************/
package fm.last.boffin.widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.TextView;
import fm.last.boffin.R;
import fm.last.boffin.db.LocalCollection.TopTagsResult;

/**
 * Layout/container for TagButtons
 * 
 * @author Lukasz Wisniewski
 */
public class TagCloud extends ViewGroup {

	public static final String TAG = "TagCloud";

	TagLayoutListener mListener;

	TreeMap<Float, TextView> mTagButtons;
	TextView mAreaHint;
	List<String> mSelectedTags = new ArrayList<String>();
	
	/**
	 * Padding between buttons
	 */
	int mPadding;

	/**
	 * Animation turned on/off
	 */
	boolean mAnimationEnabled;

	/**
	 * Indicator whether an animation is ongoing or not
	 */
	boolean mAnimating;

	/**
	 * Container for TagButton animations
	 */
	ArrayList<Animation> mAnimations;

	/**
	 * Area hint resource
	 */
	private int mAreaTextId = 0;

	public TagCloud(Context context) {
		super(context);
		init(context);
	}

	public TagCloud(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public List<String> getSelectedTags() {
		return mSelectedTags;
	}
	
	/**
	 * Sharable code between constructors
	 * 
	 * @param context
	 */
	private void init(Context context) {
		mTagButtons = new TreeMap<Float, TextView>(Collections.reverseOrder());
		mPadding = 10; // TODO get from xml layout
		mAnimationEnabled = false;
		mAnimating = false;

		mAnimations = new ArrayList<Animation>();
		// this.setFocusable(true);

		// Creating area hint
		mAreaHint = new TextView(context);
		LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		mAreaHint.setVisibility(View.GONE);
		mAreaHint.setTextColor(0xff666666);
		mAreaHint.setGravity(Gravity.CENTER);
		mAreaHint.setTextSize(16);
		mAreaHint.setTypeface(mAreaHint.getTypeface(), Typeface.BOLD);
		this.addView(mAreaHint, params);
	}
	
	public void normalizeSizes() {
		float max = 0.0f;
		float min = Float.POSITIVE_INFINITY;
		for (Float weight : mTagButtons.keySet()) {
			if( weight < min ) {
				min = weight;
			}
			if( weight > max ) {
				max = weight;
			}
		}
		float minFontSize = 10.0f;
		float maxFontSize = 100.0f;
		float multiplier = (maxFontSize-minFontSize)/(max-min);
		for (Map.Entry<Float, TextView> entry : mTagButtons.entrySet()) {
			float weight = entry.getKey();
			TextView tv = entry.getValue();
			float fontSize = minFontSize + (float)Math.log(0.01+(max-(max-(weight-min)))*multiplier);  
			tv.setTextSize( fontSize * 2.0f );
			Log.d(TAG, "Sizing: " + tv.getText() + "Weight: " + weight + " Size:" + tv.getTextSize());
		}
	}

	/**
	 * Adds tag by creating button inside TagLayout
	 * 
	 * @param tag
	 */
	public void addTag(final TopTagsResult r ) {
		final TextView tagButton = new TextView(this.getContext());
		LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		tagButton.setTextColor(Color.BLACK);
		tagButton.setTextSize(r.weight);
		tagButton.setText(r.tag);
		tagButton.setBackgroundColor(Color.LTGRAY);
		tagButton.setPadding(6, 2, 6, 2);

		tagButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				TextView tv = (TextView)v;

				if(mSelectedTags.contains(r.tag)) {
					mSelectedTags.remove(r.tag);
					tagButton.setTextColor(Color.BLACK);
					tv.setBackgroundColor(Color.LTGRAY);
				} else {
					mSelectedTags.add(r.tag);
					tagButton.setTextColor(Color.WHITE);
					tv.setBackgroundColor(Color.GRAY);
				}
			}

		});
		if (mAnimationEnabled) {
			tagButton.setVisibility(View.INVISIBLE);
		}

		mTagButtons.put(r.weight, tagButton);
		this.addView(tagButton, params);

		if (mAnimationEnabled) {
			Animation a = AnimationUtils.loadAnimation(this.getContext(), R.anim.tag_fadein);
			a.setAnimationListener(new AnimationListener() {

				public void onAnimationEnd(Animation animation) {
					tagButton.setVisibility(View.VISIBLE);
				}

				public void onAnimationRepeat(Animation animation) {
				}

				public void onAnimationStart(Animation animation) {
				}

			});
			tagButton.startAnimation(a);
		}
	}
	
	public void clear() {
		mTagButtons.clear();
	}


	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		Log.i(TAG, "onMeasue()");

		int selfw = getMeasuredWidth();
		int selfh = getMeasuredHeight();
		int x = mPadding;
		int y = mPadding;
		int maxHeight = 0;

		int count = getChildCount();

		// Area hint (on when count equals 1)
		if (count == 1 && mAreaTextId > 0) {
			mAreaHint.setVisibility(View.VISIBLE);
		} else {
			mAreaHint.setVisibility(View.GONE);
		}

		for (Map.Entry<Float, TextView> entry : mTagButtons.entrySet()) {
			TextView child = entry.getValue();

			if (child.getVisibility() != GONE) {
				// LayoutParams lp = (LayoutParams) child.getLayoutParams();
				child.measure(MeasureSpec.makeMeasureSpec(selfw, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(selfh, MeasureSpec.AT_MOST));
			}

			int cw = child.getMeasuredWidth();
			int ch = child.getMeasuredHeight();
			
			if( ch > maxHeight ) maxHeight = ch;
			
			// tag doesn't fit the row, move it to next one
			if (x + cw > selfw) {
				x = mPadding;
				y = y + maxHeight + mPadding;
				maxHeight = 0;
			}

			x = x + cw + mPadding;
		}
		
		setMeasuredDimension(selfw, y + maxHeight);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		Log.i(TAG, "onLayout()");
		if (mAnimationEnabled && mAnimating) {
			return;
		}

		int selfw = getMeasuredWidth();
		int selfh = getMeasuredHeight();

		int x = mPadding;
		int y = mPadding;
		int maxHeight = 0;

		for (Map.Entry<Float, TextView> entry : mTagButtons.entrySet()) {
			TextView child = entry.getValue();

			int cw = child.getMeasuredWidth();
			int ch = child.getMeasuredHeight();
			
			if( ch > maxHeight ) maxHeight = ch;
			
			Log.i(TAG, "child(" + entry.getValue().getText() + ") size - " + cw + "," + ch);

			// tag doesn't fit the row, move it to next one
			if (x + cw > selfw) {
				x = mPadding;
				y = y + maxHeight + mPadding;
				maxHeight = 0;
			}

			child.layout(x, y, x + cw, y + ch);

			/*if (mAnimationEnabled) {
				Animation a = child.createTranslateAnimation(400);
				if (a != null) {
					child.startAnimation(a);
				}
			}*/

			x = x + cw + mPadding;
		}

		// positioning AreaHint
		if (mAreaHint.getVisibility() == View.VISIBLE) {
			int cw = mAreaHint.getMeasuredWidth();
			int ch = mAreaHint.getMeasuredHeight();
			mAreaHint.layout((selfw - cw) / 2, (selfh - ch) / 2, (selfw + cw) / 2, (selfh + ch) / 2);
		}

	}

	public void setTagLayoutListener(TagLayoutListener l) {
		this.mListener = l;
	}

	/**
	 * Enables/disables fancy animations
	 * 
	 * @param value
	 */
	public void setAnimationsEnabled(boolean value) {
		this.mAnimationEnabled = value;
	}

	/**
	 * Sets informative text which is displayed in the middle of TagLayout area
	 * before any TagButton has been added
	 * 
	 * @param resid
	 *            resource Id
	 */
	public void setAreaHint(int resid) {
		mAreaTextId = resid;
		mAreaHint.setText(resid);
	}

}
