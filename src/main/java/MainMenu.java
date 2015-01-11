
import org.bytedeco.javacpp.Loader;
import static org.bytedeco.javacpp.helper.opencv_imgproc.cvFindContours;
import org.bytedeco.javacpp.opencv_core;
import static org.bytedeco.javacpp.opencv_core.CV_AA;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import static org.bytedeco.javacpp.opencv_core.FONT_HERSHEY_SIMPLEX;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import static org.bytedeco.javacpp.opencv_core.cvAbsDiff;
import static org.bytedeco.javacpp.opencv_core.cvFlip;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_core.cvPutText;
import static org.bytedeco.javacpp.opencv_core.cvRectangle;
import org.bytedeco.javacpp.opencv_highgui;
import static org.bytedeco.javacpp.opencv_imgproc.CV_CHAIN_APPROX_SIMPLE;
import static org.bytedeco.javacpp.opencv_imgproc.CV_GAUSSIAN;
import static org.bytedeco.javacpp.opencv_imgproc.CV_RETR_LIST;
import static org.bytedeco.javacpp.opencv_imgproc.CV_RGB2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_THRESH_BINARY;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.cvMinAreaRect2;
import static org.bytedeco.javacpp.opencv_imgproc.cvSmooth;
import static org.bytedeco.javacpp.opencv_imgproc.cvThreshold;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameGrabber;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Skrzypek
 */
public class MainMenu {

    public static CanvasFrame canvasFrame;
    private boolean soundSelected;
    private boolean twoPlayersSelected;
    private static int SOUND = 0;
    private static int EXIT = 1;
    private static int TWO_PLAYERS = 2;
    private static int START = 3;
    private static int DIMENSIONS = 4;
    private int actuallDimensions;
    private int[] buffors;
    private String[] dimensions;

    public void startMenu() throws Exception {
        reset();

        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
        grabber.start();

        opencv_core.IplImage frame = grabber.grab();
        opencv_core.IplImage image = null;
        opencv_core.IplImage grabbedImage = null;
        opencv_core.IplImage mirrorImage = null;
        opencv_core.IplImage prevImage = null;
        opencv_core.IplImage diff = null;
        opencv_highgui.CvCapture capture = opencv_highgui.cvCreateCameraCapture(0);

        this.canvasFrame = new CanvasFrame("Menu");
        canvasFrame.setCanvasSize(frame.width(), frame.height());

        opencv_core.CvMemStorage storage = opencv_core.CvMemStorage.create();

        // used to decrease options
        int counter = 0;

        while (canvasFrame.isVisible() && (frame = grabber.grab()) != null) {
            cvSmooth(frame, frame, CV_GAUSSIAN, 9, 9, 2, 2);
            if (image == null) {
                image = opencv_core.IplImage.create(frame.width(), frame.height(), IPL_DEPTH_8U, 1);
                cvCvtColor(frame, image, CV_RGB2GRAY);
            } else {
                prevImage = opencv_core.IplImage.create(frame.width(), frame.height(), IPL_DEPTH_8U, 1);
                prevImage = image;
                image = opencv_core.IplImage.create(frame.width(), frame.height(), IPL_DEPTH_8U, 1);
                cvCvtColor(frame, image, CV_RGB2GRAY);
            }

            if (diff == null) {
                diff = opencv_core.IplImage.create(frame.width(), frame.height(), IPL_DEPTH_8U, 1);
            }

            if (prevImage != null) {
                // perform ABS difference
                cvAbsDiff(image, prevImage, diff);
                // do some threshold for wipe away useless details
                cvThreshold(diff, diff, 64, 255, CV_THRESH_BINARY);
                cvFlip(diff, diff, 1);
                grabbedImage = opencv_highgui.cvQueryFrame(capture);
                mirrorImage = grabbedImage.clone();
                cvFlip(grabbedImage, mirrorImage, 1);
                drawMenu(mirrorImage);
                canvasFrame.showImage(mirrorImage);

                // recognize contours
                opencv_core.CvSeq contour = new opencv_core.CvSeq(null);
                cvFindContours(diff, storage, contour, Loader.sizeof(opencv_core.CvContour.class), CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);

                while (contour != null && !contour.isNull()) {
                    if (contour.elem_size() > 0) {
                        opencv_core.CvBox2D box = cvMinAreaRect2(contour, storage);
                        // test intersection
                        if (box != null) {
                            opencv_core.CvPoint2D32f center = box.center();
                            addToBuffor(center.x(), center.y());
                            //System.out.println(center.x() + " " + center.y());
                            opencv_core.CvSize2D32f size = box.size();
                        }
                    }
                    contour = contour.h_next();
                }
            }
        }
        grabber.stop();
        canvasFrame.dispose();

    }

    private void drawMenu(opencv_core.IplImage image) {
        opencv_core.CvFont font = new opencv_core.CvFont();

        // sound
        CvScalar cvScalar = soundSelected ? opencv_core.CvScalar.BLACK : opencv_core.CvScalar.BLUE;
        cvRectangle(image, cvPoint(0, 0), cvPoint(80, 80), cvScalar, 3, CV_AA, 0);
        opencv_core.cvLine(image, cvPoint(40, 40), cvPoint(40, 40), cvScalar, getThickness(SOUND), CV_AA, 0);
        opencv_core.cvInitFont(font, FONT_HERSHEY_SIMPLEX, 0.2, 1);
        cvPutText(image, "dzwiek", cvPoint(85, 40), font, opencv_core.CvScalar.BLUE);

        // exit
        cvRectangle(image, cvPoint(560, 0), cvPoint(640, 80), opencv_core.CvScalar.RED, 3, CV_AA, 0);
        opencv_core.cvLine(image, cvPoint(600, 40), cvPoint(600, 40), opencv_core.CvScalar.RED, getThickness(EXIT), CV_AA, 0);
        opencv_core.cvInitFont(font, FONT_HERSHEY_SIMPLEX, 0.2, 1);
        cvPutText(image, "wyjscie", cvPoint(480, 40), font, opencv_core.CvScalar.RED);

        // 2 graczy
        cvScalar = twoPlayersSelected ? opencv_core.CvScalar.BLACK : opencv_core.CvScalar.YELLOW;
        cvRectangle(image, cvPoint(0, 400), cvPoint(80, 480), cvScalar, 3, CV_AA, 0);
        opencv_core.cvLine(image, cvPoint(40, 440), cvPoint(40, 440), cvScalar, getThickness(TWO_PLAYERS), CV_AA, 0);
        opencv_core.cvInitFont(font, FONT_HERSHEY_SIMPLEX, 0.2, 1);
        cvPutText(image, "2 graczy", cvPoint(85, 460), font, opencv_core.CvScalar.YELLOW);

        // start
        cvRectangle(image, cvPoint(560, 400), cvPoint(640, 480), opencv_core.CvScalar.GREEN, 3, CV_AA, 0);
        opencv_core.cvLine(image, cvPoint(600, 440), cvPoint(600, 440), opencv_core.CvScalar.GREEN, getThickness(START), CV_AA, 0);
        opencv_core.cvInitFont(font, FONT_HERSHEY_SIMPLEX, 0.2, 1);
        cvPutText(image, "start", cvPoint(500, 460), font, opencv_core.CvScalar.GREEN);

        // dimensions
        cvRectangle(image, cvPoint(0, 200), cvPoint(80, 280), opencv_core.CvScalar.MAGENTA, 3, CV_AA, 0);
        opencv_core.cvLine(image, cvPoint(40, 240), cvPoint(40, 240), opencv_core.CvScalar.MAGENTA, getThickness(DIMENSIONS), CV_AA, 0);
        opencv_core.cvInitFont(font, FONT_HERSHEY_SIMPLEX, 0.2, 1);
        cvPutText(image, dimensions[actuallDimensions], cvPoint(85, 260), font, opencv_core.CvScalar.MAGENTA);
    }

    private void reset() {
        soundSelected = false;
        twoPlayersSelected = false;
        resetBuffors();
        dimensions = new String[]{"2x3", "3x3", "3x4"};
        actuallDimensions = 0;
    }

    private void addToBuffor(float x, float y) {
//        System.out.println(x + " " + y);
        if (x <= 80 && y <= 80) {
            increaseSound();
        }
        if (x >= 560 && y <= 80) {
            increaseExit();
        }
        if (x <= 80 && y >= 400) {
            increase2Players();
        }
        if (x >= 560 && y >= 400) {
            increaseStart();
        }
        if (x <= 80 && y >= 200 && y <=280) {
            increaseDimensions();
        }
        checkBuffors();
    }

    private void increaseSound() {
        buffors[SOUND] ++;
    }

    private void increaseExit() {
        buffors[EXIT] ++;
    }

    private void increase2Players() {
        buffors[TWO_PLAYERS] ++;
    }

    private void increaseStart() {
        buffors[START] ++;
    }

    private void increaseDimensions() {
        buffors[DIMENSIONS] ++;
    }

    private void checkBuffors() {
        if (buffors[SOUND] >= 20) {
            resetBuffors();
            soundSelected = !soundSelected;
        } else if (buffors[EXIT] >= 20) {
            this.canvasFrame.dispose();
        } else if (buffors[TWO_PLAYERS] >= 20) {
            resetBuffors();
            twoPlayersSelected = !twoPlayersSelected;
        } else if (buffors[START] >= 20) {
            resetBuffors();
            // start game
        } else if (buffors[DIMENSIONS] >= 20) {
            resetBuffors();
            actuallDimensions = actuallDimensions == 2? 0 : actuallDimensions +1;
        }
    }

    private void resetBuffors() {
        buffors = new int[5];
    }

    private int getThickness(int buffor) {
        double percentage = buffors[buffor]/30d;
        double t = 80d * percentage;
        return (int)t;
    }

    private void decreaseOptions() {
        for ( int i =0; i < 5; i ++) {
            buffors[i] --;
        }

        for ( int i =0; i < 5; i ++) {
            if (buffors[i] < 0) {
                buffors[i] = 0;
            }
        }
    }
}
