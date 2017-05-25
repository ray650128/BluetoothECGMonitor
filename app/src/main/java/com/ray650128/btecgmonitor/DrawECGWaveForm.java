package com.ray650128.btecgmonitor;

import java.util.List;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


public class DrawECGWaveForm {
	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHolder;
	private int mSFHolderHeight;
	private int mSFHolderWidth;
	private int tmpX = 0, tmpY = 0;
	//private int scaleX = 2, scaleY = 80;

	private int scaleX = 2, scaleY = 80;
	private Paint mPaint;

	int mLineWidth = 5;
	Boolean mLineAntiAlias = true;

	public DrawECGWaveForm(SurfaceView mSurfaceView)
	{
		this.mSurfaceView = mSurfaceView;
	}
	
	public void InitCanvas()
	{
		mSurfaceHolder = mSurfaceView.getHolder();
		mSFHolderHeight = mSurfaceView.getHeight();
		mSFHolderWidth = mSurfaceView.getWidth();
		mPaint = new Paint();
		mPaint.setColor(Color.RED);
		mPaint.setStrokeWidth(mLineWidth);		// 線條粗細5
		mPaint.setAntiAlias(mLineAntiAlias);	// 反鋸齒
	}
	
	public void CleanCanvas() {
		Canvas mCanvas = mSurfaceHolder.lockCanvas(
				new Rect(0, 0,
						 mSurfaceView.getWidth(),
						 mSurfaceView.getHeight())
				);
		
		mCanvas.drawColor(Color.WHITE);
		
		Paint tmpPaint = new Paint();

		tmpPaint.setStrokeWidth(1);
		tmpPaint.setColor(Color.LTGRAY);
		
		int length = mSFHolderWidth / 20;
        int bound = mSFHolderHeight / 10;
        for (int i = 0; i < bound; i++) {
            for (int j = 0; j < length; j++) {
            	mCanvas.drawLine(
            			j * length,				// X1
						0,						// Y1
						j * length,				// X2
						mSFHolderHeight,		// Y2
						tmpPaint);

            	mCanvas.drawLine(
            			0,						// X1
						i * bound,				// Y1
						mSFHolderWidth,			// X2
						i * bound,				// Y2
						tmpPaint);
            }
        }
        tmpX = 0;
		tmpY = mSFHolderHeight / 2;
		mSurfaceHolder.unlockCanvasAndPost(mCanvas);
	}
	
	public void DrawWave(List<Float> ECGDataList) {
		if (mSurfaceHolder == null) {
			this.InitCanvas();
			this.CleanCanvas();
		}
		int ptsNumber = ECGDataList.size();
		int posLst = 0;
		while(posLst < ptsNumber) {
			int posCan = 0;
			float drawPoints[] = new float[(ptsNumber) * 4];
			float ECGValue = tmpY;			
			if (tmpX == 0) {
				drawPoints[0] = 0;
				drawPoints[1] = tmpY;
			}
			else{
				drawPoints[0] = tmpX;
				drawPoints[1] = tmpY;
			}

			for (posCan = tmpX ; posCan < mSFHolderWidth && posLst < ptsNumber; posCan += scaleX) {
				try{
					ECGValue = -ECGDataList.get(posLst) * scaleY + mSFHolderHeight / 2;
				}catch(Exception e) {
					e.printStackTrace();
					posLst++;
					continue;
				}
				drawPoints[4 * posLst + 2] = posCan;
				drawPoints[4 * posLst + 3] = ECGValue;
				if(posLst < (ptsNumber - 1)) {
					drawPoints[4 * posLst + 4] = posCan;
					drawPoints[4 * posLst + 5] = ECGValue;
				}
				posLst++;
			}
			if (posCan >= mSFHolderWidth) {
				this.CleanCanvas();
			}
			else
			{
			Canvas mCanvas = mSurfaceHolder.lockCanvas(new Rect(
					tmpX, 0, posCan - scaleX, mSFHolderHeight));
			mCanvas.drawLines(drawPoints, mPaint);
			mSurfaceHolder.unlockCanvasAndPost(mCanvas);
			tmpX = posCan - scaleX;
			tmpY = (int)ECGValue; 
			}
		}
	}
}
