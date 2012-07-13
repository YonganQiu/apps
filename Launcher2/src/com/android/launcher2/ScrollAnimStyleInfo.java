package com.android.launcher2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.content.res.XmlResourceParser;
import android.util.Log;

import com.android.launcher.R;
import com.android.launcher2.Workspace.ScreenScrollAnimation;

public class ScrollAnimStyleInfo {

	private String mAnimId;
	private String mTitleId;
	private int mTitleResourceId = -1;
	private int mIconResourceId = -1;
	private String mIconId;
	private String mClassName;
	private boolean mIsDefault;
	private ScreenScrollAnimation mAnimObject;
	private static final String TAG = "Launcher.ScrollAnimStyleInfo";
	private static final String WORKSPACE_SCROLL_ANIM_XML_TAG = "anim";
	private static final String WORKSPACE_SCROLL_ANIM_XML_ATTR_ANIM_ID = "id";
	private static final String WORKSPACE_SCROLL_ANIM_XML_ATTR_TITLE_ID = "title_id";
	private static final String WORKSPACE_SCROLL_ANIM_XML_ATTR_ICON_ID = "icon_id";
	private static final String WORKSPACE_SCROLL_ANIM_XML_ATTR_CLASS_NAME = "class";
	private static final String WORKSPACE_SCROLL_ANIM_XML_ATTR_DEFAULT_ANIM = "default_anim";
	public static final String RANDOM_SCROLL_ANIM_ID = "random";

	public boolean isDefault() {
		return mIsDefault;
	}

	public void setDefault(boolean isDefault) {
		this.mIsDefault = isDefault;
	}

	public String getAnimId() {
		return mAnimId;
	}

	public void setAnimId(String animId) {
		this.mAnimId = animId;
	}

	public int getTitleId(Context context) {
		if (mTitleResourceId != -1) {
			return mTitleResourceId;
		}

		mTitleResourceId = getStringResourceId(context, mTitleId);
		return mTitleResourceId;
	}

	public void setTitleId(String titleId) {
		this.mTitleId = titleId;
	}

	public int getIconId(Context context) {
		if (mIconResourceId != -1) {
			return mIconResourceId;
		}

		mIconResourceId = getDrawableResourceId(context, mIconId);
		return mIconResourceId;
	}

	public void setIconId(String iconId) {
		this.mIconId = iconId;
	}

	public String getClassName() {
		return mClassName;
	}

	public void setClassName(String className) {
		this.mClassName = className;
	}

	public ScreenScrollAnimation getAnimObject(
			ArrayList<ScrollAnimStyleInfo> list) {
		if (RANDOM_SCROLL_ANIM_ID.equals(mAnimId)) {
			return getRandomAnimObject(list);
		}

		if (mAnimObject != null) {
			return mAnimObject;
		}

		Class<?> screenScrollAnimClass = null;
		try {
			screenScrollAnimClass = Class.forName(mClassName);
		} catch (ClassNotFoundException e) {
			Log.e(TAG, "cann't get screenScrollAnimClass. mAnimId:" + mAnimId,
					e);
			return null;
		}

		try {
			mAnimObject = (ScreenScrollAnimation) screenScrollAnimClass
					.newInstance();
		} catch (InstantiationException e) {
			Log.e(TAG, "create screenScrollAnim object is failed. mAnimId:"
					+ mAnimId, e);
			return null;
		} catch (IllegalAccessException e) {
			Log.e(TAG, "create screenScrollAnim object is failed. mAnimId:"
					+ mAnimId, e);
			return null;
		}

		return mAnimObject;

	}

	private ScreenScrollAnimation getRandomAnimObject(
			ArrayList<ScrollAnimStyleInfo> animList) {
		if (animList == null || animList.size() <= 0) {
			Log.e(TAG, "animList is null when get random anim object.");
			return null;
		}

		ArrayList<ScrollAnimStyleInfo> list = new ArrayList<ScrollAnimStyleInfo>();
		for (ScrollAnimStyleInfo anim : animList) {
			if (!RANDOM_SCROLL_ANIM_ID.equals(anim.getAnimId())) {
				list.add(anim);
			}
		}

		if (list.size() <= 0) {
			Log.e(TAG,
					"animList only have randomAnimInfo, when get random anim object.");
			return null;
		}

		Random random = new Random();
		return list.get(random.nextInt(list.size())).getAnimObject(null);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mAnimId == null) ? 0 : mAnimId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ScrollAnimStyleInfo other = (ScrollAnimStyleInfo) obj;
		if (mAnimId == null) {
			if (other.mAnimId != null)
				return false;
		} else if (!mAnimId.equals(other.mAnimId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ScrollAnimStyleInfo [mAnimId=" + mAnimId + ", mTitleId="
				+ mTitleId + ", mIconId=" + mIconId + ", mClassName="
				+ mClassName + ", mIsDefault=" + mIsDefault + "]";
	}

	public static ArrayList<ScrollAnimStyleInfo> parserWorkspaceScrollAnimConfig(
			Context context) {
		ArrayList<ScrollAnimStyleInfo> anims = new ArrayList<ScrollAnimStyleInfo>();
		try {
			XmlResourceParser xrp = context.getResources().getXml(
					R.xml.workspace_scroll_anim);
			while (xrp.next() != XmlResourceParser.START_TAG) {
				continue;
			}
			xrp.next();
			while (xrp.getEventType() != XmlResourceParser.END_TAG) {
				while (xrp.getEventType() != XmlResourceParser.START_TAG) {
					if (xrp.getEventType() == XmlResourceParser.END_DOCUMENT) {
						xrp.close();
						return anims;
					}
					xrp.next();
				}
				if (xrp.getName().equals(WORKSPACE_SCROLL_ANIM_XML_TAG)) {
					ScrollAnimStyleInfo anim = new ScrollAnimStyleInfo();
					anim.setAnimId(xrp.getAttributeValue(null,
							WORKSPACE_SCROLL_ANIM_XML_ATTR_ANIM_ID));
					anim.setTitleId(xrp.getAttributeValue(null,
							WORKSPACE_SCROLL_ANIM_XML_ATTR_TITLE_ID));
					anim.setIconId(xrp.getAttributeValue(null,
							WORKSPACE_SCROLL_ANIM_XML_ATTR_ICON_ID));
					anim.setClassName(xrp.getAttributeValue(null,
							WORKSPACE_SCROLL_ANIM_XML_ATTR_CLASS_NAME));

					String defaultAnim = xrp.getAttributeValue(null,
							WORKSPACE_SCROLL_ANIM_XML_ATTR_DEFAULT_ANIM);
					if (defaultAnim != null
							&& "true".equals(defaultAnim.trim())) {
						anim.setDefault(true);
					} else {
						anim.setDefault(false);
					}
					anims.add(anim);
				}
				while (xrp.getEventType() != XmlResourceParser.END_TAG) {
					xrp.next();
				}
				xrp.next();
			}
			xrp.close();

		} catch (NotFoundException ex) {
			Log.w(TAG, "didn't find workspace_scroll_anim.xml.", ex);
		} catch (XmlPullParserException ex) {
			Log.w(TAG, "parser workspace_scroll_anim.xml is failed.", ex);
		} catch (IOException ex) {
			Log.w(TAG, "parser workspace_scroll_anim.xml is failed.", ex);
		}
		return anims;
	}

	public static ScrollAnimStyleInfo findScrollAnimByAnimId(String animId,
			ArrayList<ScrollAnimStyleInfo> list) {
		if (list == null || list.size() <= 0 || animId == null) {
			return null;
		}

		for (ScrollAnimStyleInfo anim : list) {
			if (animId.equals(anim.getAnimId())) {
				return anim;
			}
		}
		return null;
	}

	public static ScrollAnimStyleInfo getDefaultScrollAnim(
			ArrayList<ScrollAnimStyleInfo> list) {
		if (list == null || list.size() <= 0) {
			return null;
		}

		for (ScrollAnimStyleInfo anim : list) {
			if (anim.isDefault()) {
				return anim;
			}
		}
		return null;
	}

	private static int getStringResourceId(Context context, String resoureName) {
		int resourceId = -1;
		try {
			resourceId = R.string.class.getField(resoureName).getInt(null);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "get string resource id is failed. resoureName:"
					+ resoureName, e);
			return resourceId;
		} catch (SecurityException e) {
			Log.e(TAG, "get string resource id is failed. resoureName:"
					+ resoureName, e);
			return resourceId;
		} catch (IllegalAccessException e) {
			Log.e(TAG, "get string resource id is failed. resoureName:"
					+ resoureName, e);
			return resourceId;
		} catch (NoSuchFieldException e) {
			Log.e(TAG, "get string resource id is failed. resoureName:"
					+ resoureName, e);
			return resourceId;
		} catch (Exception e) {
			Log.e(TAG, "get string resource id is failed. resoureName:"
					+ resoureName, e);
			return resourceId;
		}

		return resourceId;
	}

	private static int getDrawableResourceId(Context context, String resoureName) {
		int resourceId = -1;
		try {
			resourceId = R.drawable.class.getField(resoureName).getInt(null);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "get string resource id is failed. resoureName:"
					+ resoureName, e);
			return resourceId;
		} catch (SecurityException e) {
			Log.e(TAG, "get string resource id is failed. resoureName:"
					+ resoureName, e);
			return resourceId;
		} catch (IllegalAccessException e) {
			Log.e(TAG, "get string resource id is failed. resoureName:"
					+ resoureName, e);
			return resourceId;
		} catch (NoSuchFieldException e) {
			Log.e(TAG, "get string resource id is failed. resoureName:"
					+ resoureName, e);
			return resourceId;
		} catch (Exception e) {
			Log.e(TAG, "get string resource id is failed. resoureName:"
					+ resoureName, e);
			return resourceId;
		}

		return resourceId;
	}

}
