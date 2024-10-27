# `KamVision` :zap:
[![Medium](https://img.shields.io/badge/Medium-12100E.svg)](https://medium.com/@kamyab9k) [![Read my article](https://img.shields.io/badge/Read%20my%20article-brightgreen.svg)](https://medium.com/@kamyab9k)
![mavenCentral](https://img.shields.io/maven-central/v/com.aminography/primedatepicker?color=blue)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/c1c44ee8a3a14b0e8c963c36c8e586d8)](https://app.codacy.com/manual/aminography/PrimeDatePicker?utm_source=github.com&utm_medium=referral&utm_content=aminography/PrimeDatePicker&utm_campaign=Badge_Grade_Dashboard)
[![API](https://img.shields.io/badge/minSdkVersion-24-important.svg)](https://android-arsenal.com/api?level=24)

Firstly, **`KamVision`** is a Library that provides easy access to complex Camera 2 API in jetpack compose, you can use internal elements like `Preview` and `Frame Capturing` as a one line call in your projects.
                       ![KamVisionLogo](https://github.com/user-attachments/assets/e30e4da1-c76c-4be3-b715-aac45efee86b)

<table>

  <tr>
    <td><b>Multiple Frames | Bitmap
    <td><b>Single Frame | Bitmap
    <td><b>Start Preview | AndroidView
    <td><b>Stop Preview | AndroidView
  </tr>

  <tr>
    <td><img src="static/MCDB.gif"/></td>
    <td><img src="static/RPLB.gif"/></td>
    <td><img src="static/SHLD.gif"/></td>
    <td><img src="static/GJDD2.gif"/></td>
  </tr>

</table>

<br/>

Table of Contents
-----------------

- [Core Logic](#core-logic)
- [Main Characteristics](#main-characteristics)
- [Download](#download)
- [Usage](#usage)
   - [Library functions](#Library-functions)
   - [Input KamVision Configurations](#input-KamVision-configurations)
- [Change Log](#change-log)
<br/>

Core Logic
----------
The ❤️ of this library is provided by [**Camera2 Api**](https://developer.android.com/media/camera/camera2).

<br/>

Main Characteristics
--------------------
- Start Camera Preview with only one line function call
- Stop Camera Preview
- Single frame capturing
- Multiple frame capturing
- Full compatibality with jetpack compose
- Fluent UI
- Optimized with coroutines

#### :dart: Download [SampleApp.apk](https://github.com/aminography/PrimeDatePicker/releases/download/v3.4.0/sample-app-release.apk)

<br/>

Download
--------
**`KamVision`** is available on `MavenCentral` to download using build tools systems. Add the following lines to your `build.gradle` file:

```gradle
dependencies {
    implementation (Libs.)
    implementation (Libs.)
}
```

<br/>

Usage
-----

To enjoy `KamVision` in jetpack compose, create an instance using a builder pattern in simple 4 steps.

1. First create an  **AndroidView** and **TextureView** in a Box(Composable) to later pass it to the Library function parameters:
```kotlin
// To create textureView to pass later on
var textureView: TextureView? by remember { mutableStateOf(null) }

@Composable
fun CameraPreview() {
    val coroutineScope =
        rememberCoroutineScope()
    var textureView: TextureView? by remember { mutableStateOf(null) }

    // Use Box to ensure that the preview fills the entire screen
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                // Create the TextureView
                TextureView(ctx).also {
                    textureView = it
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { textureView ->
                   }
            }
        )
    }
}
```

2. Create instance of the Library:
```kotlin
    var cameraService: CameraService? by remember { mutableStateOf(null) }

    var textureView: TextureView? by remember { mutableStateOf(null) }
...
```

3. Build the Lib in the update block :

```kotlin
...

 update = { textureView ->
                if (cameraService == null) {
                    cameraService = CameraService.Builder(
                        context = textureView.context,
                        textureView = textureView,
                        scope = coroutineScope
                    ).build()
...
```

4. finally call the Library functions:
```kotlin
...
                    cameraService?.startPreview()

                    cameraService?.captureFrame(20)

                    cameraService!!.getCapturedFrames { frames: List<Bitmap> ->
//                  Handle captured frames here

}
...
```

<br/>


### Library functions

There are several functions you can call with KamVision.

<br/>

<table>

  <tr>
    <td><b>Function</b></td>
    <td><b>Example Usage</b></td>
  </tr>

  <tr>
    <td><b>• startPreview()</b></td>
    <td>cameraService?.startPreview()
</td>
  </tr>
  <tr>
    <td colspan="2"><i>Starts the camera preview.</i></td>
  </tr>

  <tr>
    <td><b>• stopPreview()</b></td>
    <td>cameraService?.stopPreview()</td>
  </tr>
  <tr>
    <td colspan="2"><i>Stops the camera preview.</i></td>
  </tr>

  <tr>
    <td><b>• captureFrame(Frames: Int)</b></td>
    <td>cameraService?.captureFrame(15)</td>
  </tr>
  <tr>
    <td colspan="2"><i>Capture frames and the number is given in parameter .</i></td>
  </tr>

  <tr>
    <td><b>• getCapturedFrames(frames: List<Bitmap>)</b></td>
    <td>cameraService?.getCapturedFrames { frames: List<Bitmap> ->}</td>
  </tr>
  <tr>
    <td colspan="2"><i>Returns the list of captured frames in Bitmap format.</i></td>
  </tr>




</table>

<br/>

### Input KamVision Configurations

In addition to the builder functions, `KamVision` receives some configurations. For example:

```kotlin
// shows a Preview in jetpack compose AndroidView
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                // Create the TextureView
                TextureView(ctx).also {
                    textureView = it
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { textureView ->
                if (cameraService == null) {
                    cameraService = CameraService.Builder(
                        context = textureView.context,
                        textureView = textureView,
                        scope = coroutineScope
                    ).build()
```

<br/>


Read more about these two important topics to better understand the funcionality of this Library:

- [`Camera2`](https://developer.android.com/media/camera/camera2)
- [`AndroidView`](https://developer.android.com/develop/ui/compose/migrate/interoperability-apis/views-in-compose)

You can override their parameters, or inherit a class from, or make your own theme factory.

Here is an example of how to override theme parameters to customize it:

```kotlin
val themeFactory = object : LightThemeFactory() {

    override val typefacePath: String?
        get() = "fonts/Righteous-Regular.ttf"
    
    override val dialogBackgroundColor: Int
        get() = getColor(R.color.yellow100)

    override val calendarViewBackgroundColor: Int
        get() = getColor(R.color.yellow100)

    override val pickedDayBackgroundShapeType: BackgroundShapeType
        get() = BackgroundShapeType.ROUND_SQUARE

    override val calendarViewPickedDayBackgroundColor: Int
        get() = getColor(R.color.green800)
    
    override val calendarViewPickedDayInRangeBackgroundColor: Int
        get() = getColor(R.color.green400)

    override val calendarViewPickedDayInRangeLabelTextColor: Int
        get() = getColor(R.color.gray900)

    override val calendarViewTodayLabelTextColor: Int
        get() = getColor(R.color.purple200)

    override val calendarViewWeekLabelFormatter: LabelFormatter
        get() = { primeCalendar ->
            when (primeCalendar[Calendar.DAY_OF_WEEK]) {
                Calendar.SATURDAY,
                Calendar.SUNDAY -> String.format("%s😍", primeCalendar.weekDayNameShort)
                else -> String.format("%s", primeCalendar.weekDayNameShort)
            }
        }

    override val calendarViewWeekLabelTextColors: SparseIntArray
        get() = SparseIntArray(7).apply {
            val red = getColor(R.color.red300)
            val indigo = getColor(R.color.indigo500)
            put(Calendar.SATURDAY, red)
            put(Calendar.SUNDAY, red)
            put(Calendar.MONDAY, indigo)
            put(Calendar.TUESDAY, indigo)
            put(Calendar.WEDNESDAY, indigo)
            put(Calendar.THURSDAY, indigo)
            put(Calendar.FRIDAY, indigo)
        }

    override val calendarViewShowAdjacentMonthDays: Boolean
        get() = true

    override val selectionBarBackgroundColor: Int
        get() = getColor(R.color.brown600)

    override val selectionBarRangeDaysItemBackgroundColor: Int
        get() = getColor(R.color.orange700)
}
```

<br/>


In Development
----------

- Single image capturing 
<br/>

License
--------
```
Copyright 2024 kamyab.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  ADD apache link licence

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
