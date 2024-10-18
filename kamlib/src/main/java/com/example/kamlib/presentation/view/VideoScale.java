package com.example.kamlib.presentation.view;


import android.graphics.Matrix;

public class VideoScale {
    public static final int FILL = 0;
    public static final int FIT_CENTER = 1;
    public static final int CROP_CENTER = 2;

    private static float getDiffX(float viewWidth, float videoWidth) {
        return (viewWidth > videoWidth) ? viewWidth / videoWidth : videoWidth / viewWidth;
    }

    private static float getDiffY(float viewHeight, float videoHeight) {
        return (viewHeight > videoHeight) ? viewHeight / videoHeight : videoHeight / viewHeight;
    }

    private static float getAspectRatio(float width, float height) {
        return width / height;
    }

    private static Matrix createScaleMatrix(float scaleX, float scaleY, float width, float height) {
        Matrix matrix = new Matrix();
        matrix.setScale(scaleX, scaleY, width / 2, height / 2);
        return matrix;
    }

    public static Matrix getVideoScaleMatrix(float viewWidth, float viewHeight, float videoWidth, float videoHeight, int scaleType) {
        float diffX = getDiffX(viewWidth, videoWidth);
        float diffY = getDiffY(viewHeight, videoHeight);
        float videoAspectRatio = getAspectRatio(videoWidth, videoHeight);
        if (scaleType == FIT_CENTER) {
            // Calculate the view scale with crop center
            float scaleX = getFitCenterX(viewWidth, viewHeight, videoWidth, videoHeight, diffX, diffY, videoAspectRatio);
            float scaleY = getFitCenterY(viewWidth, viewHeight, videoWidth, videoHeight, diffX, diffY, videoAspectRatio);
            return createScaleMatrix(scaleX, scaleY, viewWidth, viewHeight);
        } else if (scaleType == CROP_CENTER) {
            // Calculate the view scale with crop center
            float scaleX = getCropCenterX(viewWidth, viewHeight, videoWidth, videoHeight, diffX, diffY, videoAspectRatio);
            float scaleY = getCropCenterY(viewWidth, viewHeight, videoWidth, videoHeight, diffX, diffY, videoAspectRatio);
            return createScaleMatrix(scaleX, scaleY, viewWidth, viewHeight);
        } else if (scaleType == FILL) {
            return null;
        }
        return null;
    }

    private static float getFitCenterX(float viewWidth, float viewHeight, float videoWidth, float videoHeight, float diffX, float diffY, float videoAspectRatio) {
        float scaleX;
        if (viewWidth < videoWidth) {
            if (viewHeight < videoHeight) {
                if (diffX > diffY) {
                    scaleX = 1;
                } else {
                    scaleX = (viewHeight * videoAspectRatio) / viewWidth;
                }
            } else {
                scaleX = 1;
            }
        } else {
            if (viewHeight < videoHeight) {
                scaleX = (viewHeight * videoAspectRatio) / viewWidth;
            } else {
                if (diffX >= diffY) {
                    scaleX = (viewHeight * videoAspectRatio) / viewWidth;
                } else {
                    scaleX = 1;
                }
            }
        }
        return scaleX;
    }

    private static float getFitCenterY(float viewWidth, float viewHeight, float videoWidth, float videoHeight, float diffX, float diffY, float videoAspectRatio) {
        float scaleY;
        if (viewHeight < videoHeight) {
            if (viewWidth < videoWidth) {
                if (diffY > diffX) {
                    scaleY = 1;
                } else {
                    scaleY = (viewWidth / videoAspectRatio) / viewHeight;
                }
            } else {
                scaleY = 1;
            }
        } else {
            if (viewWidth < videoWidth) {
                scaleY = (viewWidth / videoAspectRatio) / viewHeight;
            } else {
                if (diffY > diffX) {
                    scaleY = (viewWidth / videoAspectRatio) / viewHeight;
                } else {
                    scaleY = 1;
                }
            }
        }
        return scaleY;
    }

    private static float getCropCenterX(float viewWidth, float viewHeight, float videoWidth, float videoHeight, float diffX, float diffY, float videoAspectRatio) {
        float scaleX;
        if (viewWidth < videoWidth) {
            if (viewHeight < videoHeight) {
                if (diffX > diffY) {
                    scaleX = (viewHeight * videoAspectRatio) / viewWidth;
                } else {
                    scaleX = 1;
                }
            } else {
                scaleX = (viewHeight * videoAspectRatio) / viewWidth;
            }
        } else {
            if (viewHeight < videoHeight) {
                scaleX = 1;
            } else {
                if (diffX >= diffY) {
                    scaleX = 1;
                } else {
                    scaleX = (viewHeight * videoAspectRatio) / viewWidth;
                }
            }
        }
        return scaleX;
    }

    private static float getCropCenterY(float viewWidth, float viewHeight, float videoWidth, float videoHeight, float diffX, float diffY, float videoAspectRatio) {
        float scaleY;
        if (viewHeight < videoHeight) {
            if (viewWidth < videoWidth) {
                if (diffY > diffX) {
                    scaleY = (viewWidth / videoAspectRatio) / viewHeight;
                } else {
                    scaleY = 1;
                }
            } else {
                scaleY = (viewWidth / videoAspectRatio) / viewHeight;
            }
        } else {
            if (viewWidth < videoWidth) {
                scaleY = 1;
            } else {
                if (diffY > diffX) {
                    scaleY = 1;
                } else {
                    scaleY = (viewWidth / videoAspectRatio) / viewHeight;
                }
            }
        }
        return scaleY;
    }
}