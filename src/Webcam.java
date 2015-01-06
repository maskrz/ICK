
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bytedeco.javacpp.Loader;
import static org.bytedeco.javacpp.helper.opencv_objdetect.cvHaarDetectObjects;
import org.bytedeco.javacpp.opencv_core;
import static org.bytedeco.javacpp.opencv_core.CV_AA;
import org.bytedeco.javacpp.opencv_core.CvFont;
import org.bytedeco.javacpp.opencv_core.CvMemStorage;
import org.bytedeco.javacpp.opencv_core.CvRect;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.CvSeq;
import static org.bytedeco.javacpp.opencv_core.FONT_HERSHEY_SIMPLEX;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import org.bytedeco.javacpp.opencv_core.IplImage;
import static org.bytedeco.javacpp.opencv_core.cvClearMemStorage;
import static org.bytedeco.javacpp.opencv_core.cvFlip;
import static org.bytedeco.javacpp.opencv_core.cvGetSeqElem;
import static org.bytedeco.javacpp.opencv_core.cvLoad;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_core.cvPutText;
import static org.bytedeco.javacpp.opencv_core.cvRectangle;
import org.bytedeco.javacpp.opencv_highgui;
import org.bytedeco.javacpp.opencv_highgui.CvCapture;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import org.bytedeco.javacpp.opencv_objdetect;
import static org.bytedeco.javacpp.opencv_objdetect.CV_HAAR_DO_CANNY_PRUNING;
import org.bytedeco.javacpp.opencv_objdetect.CvHaarClassifierCascade;
import org.bytedeco.javacv.CanvasFrame;

public class Webcam {

    public static int[][] board;
    public static final int rows = 2;
    public static final int columns = 3;
    public static final double scaleX = 640 / columns;
    public static final double scaleY = 400 / rows;
    public static double actuallMinX;
    public static double actuallMaxX;
    public static double actuallMinY;
    public static double actuallMaxY;
    public static int actuallRow;
    public static int actuallColumn;
    public static long start;
    public static long end;
    public static double[] records;

    Records r;
    public static CanvasFrame frame;

    public static void main(String[] args) throws Exception {
        startMenu();
        //start();
    }

    public static void startMenu() {

        CvCapture capture = opencv_highgui.cvCreateCameraCapture(0);

        opencv_highgui.cvSetCaptureProperty(capture,
                opencv_highgui.CV_CAP_PROP_FRAME_HEIGHT, 400);
        opencv_highgui.cvSetCaptureProperty(capture,
                opencv_highgui.CV_CAP_PROP_FRAME_WIDTH, 500);

        IplImage grabbedImage = opencv_highgui.cvQueryFrame(capture);
        IplImage mirrorImage = grabbedImage.clone();
        IplImage grayImage = IplImage.create(mirrorImage.width(),
                mirrorImage.height(), IPL_DEPTH_8U, 1);

        frame = new CanvasFrame("Menu", 1);
        while (frame.isVisible()
                && (grabbedImage = opencv_highgui.cvQueryFrame(capture))
                != null) {


            // Flip the image because a mirror image looks more natural
            cvFlip(grabbedImage, mirrorImage, 1);
            // Create a black and white image - best for face detection
            // according to OpenCV sample.
            cvCvtColor(mirrorImage, grayImage, CV_BGR2GRAY);

            // display mirrorImage on frame
            drawMenu(mirrorImage);
            frame.showImage(mirrorImage);
        }
    }

    public static void start() {
        restartGame();

        // Load object detection
        Loader.load(opencv_objdetect.class);

        // Construct classifiers from xml.
        CvHaarClassifierCascade faceClassifier = loadHaarClassifier(
                "D:\\opencv\\opencv\\sources\\data\\haarcascades\\haarcascade_frontalface_alt.xml");

        CvCapture capture = opencv_highgui.cvCreateCameraCapture(0);

        opencv_highgui.cvSetCaptureProperty(capture,
                opencv_highgui.CV_CAP_PROP_FRAME_HEIGHT, 400);
        opencv_highgui.cvSetCaptureProperty(capture,
                opencv_highgui.CV_CAP_PROP_FRAME_WIDTH, 500);

        IplImage grabbedImage = opencv_highgui.cvQueryFrame(capture);
        IplImage mirrorImage = grabbedImage.clone();
        IplImage grayImage = IplImage.create(mirrorImage.width(),
                mirrorImage.height(), IPL_DEPTH_8U, 1);

        CvMemStorage faceStorage = CvMemStorage.create();

        frame = new CanvasFrame("Super gra!!!", 1);

        while (frame.isVisible()
                && (grabbedImage = opencv_highgui.cvQueryFrame(capture))
                != null) {

            // Clear out storage
            cvClearMemStorage(faceStorage);

            // Flip the image because a mirror image looks more natural
            cvFlip(grabbedImage, mirrorImage, 1);
            // Create a black and white image - best for face detection
            // according to OpenCV sample.
            cvCvtColor(mirrorImage, grayImage, CV_BGR2GRAY);

            findFace(faceClassifier, faceStorage,
                    CvScalar.GREEN, mirrorImage, mirrorImage);

            // display mirrorImage on frame
            drawBoar(mirrorImage);
            frame.showImage(mirrorImage);
        }

        // display captured image on frame
        frame.dispose();
        opencv_highgui.cvReleaseCapture(capture);
    }

    /**
     * Find objects matching the supplied Haar classifier.
     *
     * @param classifier The Haar classifier for the object we're looking for.
     * @param storage In-memory storage to use for computations
     * @param colour Colour of the marker used to make objects found.
     * @param inImage Input image that we're searching.
     * @param outImage Output image that we're going to mark and display.
     */
    private static void findFace(
            CvHaarClassifierCascade classifier,
            CvMemStorage storage,
            CvScalar colour,
            IplImage inImage,
            IplImage outImage) {

//        cvFlip(inImage, inImage, 1);
        CvSeq faces = cvHaarDetectObjects(inImage, classifier,
                storage, 1.1, 7, CV_HAAR_DO_CANNY_PRUNING);
        int totalFaces = faces.total();
        //captureImage();

        for (int i = 0; i < totalFaces; i++) {
            CvRect r = new CvRect(cvGetSeqElem(faces, i));
            int x = r.x(), y = r.y(), w = r.width(), h = r.height();
            cvRectangle(outImage, cvPoint(x, y), cvPoint(x + w, y + h),
                    colour, 2, CV_AA, 0);
            /*
             System.out.println("----------------------");
             System.out.println(x + " " + y);
             System.out.println(w + " " + h);
             System.out.println(actuallMinX + " " + actuallMinY);
             System.out.println(actuallMaxX + " " + actuallMaxY);
             */
            if (y > actuallMinX && x > actuallMinY && h + y < actuallMaxX && w + x < actuallMaxY) {
                board[actuallRow][actuallColumn] = 1;
                randomField();
            }
        }
    }

    /**
     * Load a Haar classifier from its xml representation.
     *
     * @param classifierName Filename for the haar classifier xml.
     * @return a Haar classifier object.
     */
    private static CvHaarClassifierCascade loadHaarClassifier(
            String classifierName) {

        CvHaarClassifierCascade classifier = new CvHaarClassifierCascade(
                cvLoad(classifierName));
        if (classifier.isNull()) {
            System.err.println("Error loading classifier file \"" + classifier
                    + "\".");
            System.exit(1);
        }

        return classifier;
    }

    private static void drawBoar(IplImage mirrorImage) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                if (board[i][j] == -1) {
                    cvRectangle(mirrorImage, cvPoint((int) (j * scaleX), (int) (i * scaleY)),
                            cvPoint((int) (j * scaleX + scaleX), (int) (i * scaleY + scaleY)),
                            CvScalar.BLACK, -1, CV_AA, 0);
                }
            }
        }

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                if (board[i][j] == 0) {
                    cvRectangle(mirrorImage, cvPoint((int) (j * scaleX), (int) (i * scaleY)),
                            cvPoint((int) (j * scaleX + scaleX), (int) (i * scaleY + scaleY)),
                            CvScalar.BLUE, 3, CV_AA, 0);
                }
            }
        }
    }

    private static void drawMenu(IplImage image) {
        CvFont font = new opencv_core.CvFont();

        // sound
        cvRectangle(image, cvPoint(0, 0), cvPoint(60, 60), CvScalar.BLUE, 3, CV_AA, 0);
        opencv_core.cvInitFont(font, FONT_HERSHEY_SIMPLEX, 0.2, 1);
        cvPutText(image, "dzwiek", cvPoint(65, 30), font, CvScalar.BLUE);

        // exit
        cvRectangle(image, cvPoint(580, 0), cvPoint(640, 60), CvScalar.RED, 3, CV_AA, 0);
        opencv_core.cvInitFont(font, FONT_HERSHEY_SIMPLEX, 0.2, 1);
        cvPutText(image, "wyjscie", cvPoint(500, 30), font, CvScalar.RED);

        // 2 graczy
        cvRectangle(image, cvPoint(0, 340), cvPoint(60, 400), CvScalar.YELLOW, 3, CV_AA, 0);
        opencv_core.cvInitFont(font, FONT_HERSHEY_SIMPLEX, 0.2, 1);
        cvPutText(image, "2 graczy", cvPoint(65, 380), font, CvScalar.YELLOW);

        // start
        cvRectangle(image, cvPoint(580, 340), cvPoint(640, 400), CvScalar.GREEN, 3, CV_AA, 0);
        opencv_core.cvInitFont(font, FONT_HERSHEY_SIMPLEX, 0.2, 1);
        cvPutText(image, "start", cvPoint(500, 380), font, CvScalar.GREEN);
    }

    private static void randomField() {
        int counterBlack = 0;
        int counterGood = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                if (board[i][j] == -1) {
                    counterBlack++;
                }
                if (board[i][j] == 1) {
                    counterGood++;
                }
            }
        }
        if (counterGood == rows * columns && end < 0) {
            processWin();
        } else {
            int num = (int) (Math.random() * counterBlack);
            counterBlack = 0;
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < columns; j++) {
                    if (board[i][j] == -1) {
                        if (counterBlack == num) {
                            recalcActualls(i, j);
                        }
                        counterBlack++;
                    }
                }

            }
        }

    }

    private static void recalcActualls(int i, int j) {
        actuallRow = i;
        actuallColumn = j;
        board[i][j] = 0;
        actuallMinX = i * scaleY;
        actuallMaxX = i * scaleY + scaleY;
        actuallMinY = j * scaleX;
        actuallMaxY = j * scaleX + scaleX;
    }

    private static void displayBoard() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                System.out.print(board[i][j] + " ");
            }
            System.out.println("");
        }
    }

    public static void restartGame() {
        board = new int[rows][columns];
        for (int i = 0; i < rows; i++) {
            Arrays.fill(board[i], -1);
        }
        recalcActualls((int) (Math.random()*rows), (int) (Math.random()*columns));
        start = System.currentTimeMillis();
        end = -1;
    }

    private static void processWin() {
        end = System.currentTimeMillis();
        System.out.println("WYGRANA!!!");
        long time = (end - start) / 10;
        System.out.println(time);
        double result = time / 100d;
        System.out.println(result);
        frame.dispose();
        showResults(result);
    }

    private static void showResults(double result) {
        try {
            records = new double[6];
            Arrays.fill(records, 999);
            File f = new File("records"+rows+columns+".txt");
            Scanner sc = new Scanner(f);
            int i = 0;
            while (sc.hasNextLine()) {
                records[i] = Double.valueOf(sc.nextLine());
                i++;
            }
            sc.close();
            records[5] = result;
            Arrays.sort(records);
            double rec[] = Arrays.copyOfRange(records, 0, 5);
            updateResults(rec);
            Records r = new Records(rec, result);

        } catch (FileNotFoundException ex) {
            Logger.getLogger(Webcam.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void updateResults(double[] records) {
        FileWriter fw = null;
        try {
            File f = new File("records"+rows+columns+".txt");
            fw = new FileWriter(f);
            for (int i = 0; i < 5; i++) {
                fw.append(records[i] + System.getProperty("line.separator"));
            }
        } catch (IOException ex) {
            Logger.getLogger(Webcam.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fw.close();
            } catch (IOException ex) {
                Logger.getLogger(Webcam.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
