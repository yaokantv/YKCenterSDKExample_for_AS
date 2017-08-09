package com.ykan.sdk.example.other;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.ImageView;

import com.yaokan.sdk.utils.ResourceManager;
import com.yaokan.sdk.utils.Utility;

@SuppressLint("NewApi")
public class AnimStudy {

	private Drawable oldDrawable;
	
	private boolean codeStudying = false ;
	
	private View currentView ;
	
	private AnimationDrawable anim;
	
	private String currResStr;
	
	private Context ctx;
	
	public AnimStudy(Context ctx){
		this.ctx = ctx;
	}
	
	@SuppressWarnings("deprecation")
	public void startAnim(View view){
		if(!codeStudying){
			 currentView = view ;
			 currResStr = (String) view.getTag();
			 if(view instanceof ImageView){
				 oldDrawable =((ImageView)view).getDrawable();
			 }else {
				 oldDrawable = view.getBackground();
			}		 
			 anim =  new AnimationDrawable(); 
			 anim.setOneShot(false);
			 try {
				 anim.addFrame(ctx.getResources().getDrawable(ResourceManager.getIdByName(ctx,ResourceManager.drawable, "yk_ctrl_unselected_" + currResStr)), 300);
				 anim.addFrame(ctx.getResources().getDrawable(ResourceManager.getIdByName(ctx,ResourceManager.drawable, "yk_ctrl_selected_" + currResStr)), 300);
				 view.setBackground(anim);
				 anim.start(); 
				codeStudying = true ;
			} catch (NotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
	public GradientDrawable getGradientDrawable(int strokeWidth,int strokeColor,int fillColor){
	    GradientDrawable gd = new GradientDrawable();//创建drawable
	    gd.setColor(fillColor);
	    gd.setShape(GradientDrawable.OVAL);
	    gd.setSize(100, 100);
	    gd.setStroke(strokeWidth, strokeColor);
	    return gd;
	}
	
	public void stopAnim(int status){
	  if(!Utility.isEmpty(currentView)){
		  anim.stop();
		  if(currentView instanceof ImageView){
			  ((ImageView)currentView).setImageDrawable(oldDrawable);
			 }else {
				  currentView.setBackground(oldDrawable);
			}
		  codeStudying = false;
	  }
	}
	
}
