package fr.free.nrw.commons.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Color;
import android.graphics.Rect;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.imgproc.Imgproc;

import timber.log.Timber;

/**
 * Created by bluesir9 on 3/10/17.
 */

public class ImageUtils {
    //atleast 50% of the image in question should be considered dark for the entire image to be dark
    private static final double MINIMUM_DARKNESS_FACTOR = 0.50;
    //atleast 50% of the image in question should be considered blurry for the entire image to be blurry
    private static final double MINIMUM_BLURRYNESS_FACTOR = 0.50;
    private static final int LAPLACIAN_VARIANCE_THRESHOLD = 70;

    public enum Result {
        IMAGE_DARK,
        IMAGE_BLURRY,
        IMAGE_OK
    }

    /**
     * BitmapRegionDecoder allows us to process a large bitmap by breaking it down into smaller rectangles. The rectangles
     * are obtained by setting an initial width, height and start position of the rectangle as a factor of the width and
     * height of the original bitmap and then manipulating the width, height and position to loop over the entire original
     * bitmap. Each individual rectangle is independently processed to check if its bright enough or too blurry. Based on
     * the factor of "bright enough" or "blurry" individual rectangles amongst the total rectangles into which the image
     * was divided, we will declare the image as wanted/unwanted
     *
     * @param bitmapRegionDecoder BitmapRegionDecoder for the image we wish to process
     * @return Result.IMAGE_OK if image is neither dark nor blurry or if the input bitmapRegionDecoder provided is null
     *         Result.IMAGE_DARK if image is dark
     *         Result.IMAGE_BLURRY if image is blurry
     */
    public static Result checkIfImageIsDarkOrBlurry(BitmapRegionDecoder bitmapRegionDecoder) {
        if (bitmapRegionDecoder == null) {
            Timber.e("Expected bitmapRegionDecoder was null");
            return Result.IMAGE_OK;
        }

        int loadImageHeight = bitmapRegionDecoder.getHeight();
        int loadImageWidth = bitmapRegionDecoder.getWidth();

        int checkImageTopPosition = 0;
        int checkImageBottomPosition = loadImageHeight / 10;
        int checkImageLeftPosition = 0;
        int checkImageRightPosition = loadImageWidth / 10;

        int totalDividedRectangles = 0;
        int numberOfDarkRectangles = 0;
        int numberOfBlurredRectangles = 0;

        while ((checkImageRightPosition <= loadImageWidth) && (checkImageLeftPosition < checkImageRightPosition)) {
            while ((checkImageBottomPosition <= loadImageHeight) && (checkImageTopPosition < checkImageBottomPosition)) {
                Timber.d("left: " + checkImageLeftPosition + " right: " + checkImageRightPosition + " top: " + checkImageTopPosition + " bottom: " + checkImageBottomPosition);

                Rect rect = new Rect(checkImageLeftPosition,checkImageTopPosition,checkImageRightPosition,checkImageBottomPosition);
                totalDividedRectangles++;

                Bitmap processBitmap = bitmapRegionDecoder.decodeRegion(rect,null);

                if (checkIfImageIsDark(processBitmap)) {
                    numberOfDarkRectangles++;
                }

                if (checkIfImageIsBlurred(processBitmap)) {
                    numberOfBlurredRectangles++;
                }
                checkImageTopPosition = checkImageBottomPosition;
                checkImageBottomPosition += (checkImageBottomPosition < (loadImageHeight - checkImageBottomPosition)) ? checkImageBottomPosition : (loadImageHeight - checkImageBottomPosition);
            }

            checkImageTopPosition = 0; //reset to start
            checkImageBottomPosition = loadImageHeight / 10; //reset to start
            checkImageLeftPosition = checkImageRightPosition;
            checkImageRightPosition += (checkImageRightPosition < (loadImageWidth - checkImageRightPosition)) ? checkImageRightPosition : (loadImageWidth - checkImageRightPosition);
        }

        Timber.d("dark rectangles count = " + numberOfDarkRectangles + ", total rectangles count = " + totalDividedRectangles);
        Timber.d("blurred rectangles count = " + numberOfBlurredRectangles + ", total rectangles count = " + totalDividedRectangles);


        if (numberOfDarkRectangles > totalDividedRectangles * MINIMUM_DARKNESS_FACTOR) {
            return Result.IMAGE_DARK;
        }

        if (numberOfBlurredRectangles > totalDividedRectangles * MINIMUM_BLURRYNESS_FACTOR) {
            return Result.IMAGE_BLURRY;
        }

        return Result.IMAGE_OK;
    }

    /**
     * Pulls the pixels into an array and then runs through it while checking the brightness of each pixel.
     * The calculation of brightness of each pixel is done by extracting the RGB constituents of the pixel
     * and then applying the formula to calculate its "Luminance". If this brightness value is less than
     * 50 then the pixel is considered to be dark. Based on the MINIMUM_DARKNESS_FACTOR if enough pixels
     * are dark then the entire bitmap is considered to be dark.
     *
     * <p>For more information on this brightness/darkness calculation technique refer the accepted answer
     * on this -> https://stackoverflow.com/questions/35914461/how-to-detect-dark-photos-in-android/35914745
     * SO question and follow the trail.
     *
     * @param bitmap The bitmap that needs to be checked.
     * @return true if bitmap is dark or null, false if bitmap is bright
     */
    private static boolean checkIfImageIsDark(Bitmap bitmap) {
        if (bitmap == null) {
            Timber.e("Expected bitmap was null");
            return true;
        }

        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();

        int allPixelsCount = bitmapWidth * bitmapHeight;
        int[] bitmapPixels = new int[allPixelsCount];

        bitmap.getPixels(bitmapPixels,0,bitmapWidth,0,0,bitmapWidth,bitmapHeight);
        boolean isImageDark = false;
        int darkPixelsCount = 0;

        for (int pixel : bitmapPixels) {
            int r = Color.red(pixel);
            int g = Color.green(pixel);
            int b = Color.blue(pixel);

            int brightness = (int) (0.2126 * r + 0.7152 * g + 0.0722 * b);
            if (brightness < 50) {
                //pixel is dark
                darkPixelsCount++;
                if (darkPixelsCount > allPixelsCount * MINIMUM_DARKNESS_FACTOR) {
                    isImageDark = true;
                    break;
                }
            }
        }

        return isImageDark;
    }

    /**
     * Processes the bitmap provided to see if its blurry or not.
     *
     * <p>The OpenCV library is utilised to calculate the variance of the Laplacian of
     * a grayscale version of the original bitmap.
     *
     * <p>A small excerpt from the article here -> https://www.pyimagesearch.com/2015/09/07/blur-detection-with-opencv/
     * which was used as reference, explaining why this technique works:
     *
     * <p>"The reason this method works is due to the definition of the Laplacian operator itself, which is used to measure
     * the 2nd derivative of an image. The Laplacian highlights regions of an image containing rapid intensity changes,
     * much like the Sobel and Scharr operators. And, just like these operators, the Laplacian is often used for edge detection.
     * The assumption here is that if an image contains high variance then there is a wide spread of responses,
     * both edge-like and non-edge like, representative of a normal, in-focus image. But if there is very low variance,
     * then there is a tiny spread of responses, indicating there are very little edges in the image. As we know,
     * the more an image is blurred, the less edges there are."
     *
     * @param bitmap The bitmap that needs to be checked.
     * @return true if bitmap is blurry or null, false if bitmap is not blurry
     */
    private static boolean checkIfImageIsBlurred(Bitmap bitmap) {
        if (bitmap == null) {
            Timber.e("Expected bitmap was null");
            return false;
        }

        Mat imageBitmapMat = new Mat(bitmap.getWidth(),bitmap.getHeight(),CvType.CV_8UC1);
        Utils.bitmapToMat(bitmap,imageBitmapMat);

        Mat grayscaleBitmapMat = new Mat();
        Imgproc.cvtColor(imageBitmapMat,grayscaleBitmapMat,Imgproc.COLOR_RGB2GRAY);

        Mat postLaplacianMat = new Mat();
        Imgproc.Laplacian(grayscaleBitmapMat,postLaplacianMat,3);

        MatOfDouble mean = new MatOfDouble();
        MatOfDouble standardDeviation = new MatOfDouble();
        Core.meanStdDev(postLaplacianMat,mean,standardDeviation);

        double result = Math.pow(standardDeviation.get(0,0)[0],2);
        Timber.d("blurry result = " + result);
        return result < LAPLACIAN_VARIANCE_THRESHOLD;   //true if image is blurry, false otherwise
    }
}
