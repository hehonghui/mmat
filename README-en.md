# ![](./doc/mmat_icon.png) MMAT - Android Automatic Memory Analyzer Tool

[ ![Download](https://api.bintray.com/packages/bboyfeiyu/maven/mmat/images/download.svg?version=1.0) ](https://bintray.com/bboyfeiyu/maven/mmat/1.0/link)
[![CircleCI](https://circleci.com/gh/hehonghui/mmat.svg?style=svg)](https://circleci.com/gh/hehonghui/mmat)

<a href="https://tracking.gitads.io/?repo=mmat"> <img src="https://images.gitads.io/mmat" alt="GitAds"/> </a>

When developing an Android App, we tend to use `LeakCanary` to detect memory leak, the basic principle of which is detecting if there is any memory leak regarding Activity and Fragment during App running. If there is indeed memory leak, then `LeakCanary` will proceed dump hprof and analyze the GC ROOT of the leaked object. And then notification will be delivered to user to inform them about the leak. It is a simple and efficient way to locate the memory leak during App development. 

But `LeakCanary` dumps memory snapshot right after detecting leak during app running, and carrys out data analysis. And due to limited calculating capacity of mobile device, it is not able to detect all the existing leaks. For example, if `LeakCanary` is analyzing one leak and there is another one at the same time, and the user has logged out the app, then some part of the leaks may not be detected.  

Therefore, we will need another `supplementary method` to thoroughly and automatically analyze the memory leak of an App after running. Analyzing all the memory leak of Activity and Fragment at one time will make it more complete and efficient.

And voila, MMAT. Its main idea is to use adb shell command to go back to the main page after the user finishes operating the app (either operation by you or random operation by Monkey runner), and the go back to the phone desktop (By this time the App is still alive but all the Activities and Fragments should be destroyed and the app is running backstage). Now, if there is no memory leak, then there will be no instance of Activity and Fragments. The next step is to dump memory snapshot to PC to proceed offline analysis using MMAT so as to get the complete memory leak report.


## 一、Workflow of MMAT

1. If Monkey test command is set, then run the Monkey test (Monkey test will allow the App enter various Activities randomly and this kind of pressure test tends to generate memory leak)
	* 1.1 start Monkey test
	* 1.2 go back to App main page
	* 1.3 return App to backstage and then phone desktop
	* 1.4 execute the App's force gc (your phone needs to be rooted)
2. If you don't want to Monkey test, you can also operate your App manually. After finishing all the operations, return the App to backstage.
3. run MMAT, dump hprof memory snapshot
4. Analyze hprof, and receive the leak record of all Activities and Fragments.
5. Save the analysis result as html report.


## 二、Usage of MMAT

> Note: MMAT will use adb command to dump memory snapshot when operating the App by Monkey runner, so if you need to dump the memory snapshot of release version, please make sure your App is debuggable when tested by MMAT. That means, you need to add 'android:debuggable="true"' to AndroidManifest.xml in the application tag. (**Risk alert: we suggest that you set debuggable=true during testing, but set it as false in release apk**).

There are two ways of using MMAT, please refer to [section 2.1](#mmat-plugin) and [section 2.2](#mmat-jar) .


<span id='mmat-plugin'></span>

### 2.1 Use MMAT through gradle plugin
  
* add `classpath 'org.mrcd:mmat-plugin:0.9.1'` into project build.gradle. For example: 

```gradle
buildscript {

    repositories {
        // ...
    }
    
    dependencies {
        classpath 'com.android.tools.build:gradle:3.4.1'

        // add mmat plugin library
        classpath 'org.mrcd:mmat-plugin:0.9.2'
    }
}
```

* apply `mmat-plugin` in `build.gradle` of app module, and make related configuration of MMAT. For exmaple: 

```gradle
apply plugin: 'com.mrcd.mmat.plugin'


// configuration of mmat plugin 
mmat {
    // json config file
    jsonConfigFile 'app/mmat-config.json'
    // disable Monkey runner
    disableMonkey false
    
    // If hprofFile is set, the monkey test command will be ignored.
    // hprofFile "your-hprof-file-path"
}
```

* execute `./gradlew startMmatRunner` to analyze memory leak automatically, and then final report will be saved in `hprof_analysis/report/`. see [Hprof Analysis Report](#report).


<span id='mmat-jar'></span>

### 2.2  Use MMAT through jar

Save [mmat-1.0.jar](./dist/mmat-1.0.jar) into the root dir of your project, and then add `mmat-config.json`, see [mmat configuration](#mmat-config) for reference. The next step is to run executable mmat jar in the root dir of your project. For example:

`java  -jar  mmat-1.0.jar /User/mrsimple/test-project/mmat-config.json`


<span id='report'></span>
After executing you will see the report in `/User/mrsimple/test-project/hprof-analysis/report`. see the picture below:    

<img src="./doc/leak-report.png" width="720" />

In the picture, it lists the Activity, its instance address, retained memory size, and GC ROOT. So you can see which pages have leaks and their size, and where to fix it.

For example, the GC ROOT of the first memory leak record in the picture is `static com.example.mmat.MemoryLeakActivity.sActivityLeaked`, `sActivityLeaked` is a static feild in MemoryLeakActivity class, it holds the reference of the MemoryLeakActivity instance. From reference link you can see the `sActivityLeaked ` should be a LinkedList class, and the MemoryLeakActivity instance is one of its element.

Have a look at related code as below: 

```java
/**
 * memory leak of Page
 */
public class MemoryLeakActivity extends AppCompatActivity {

    private static List<Activity> sActivityLeaked = new LinkedList<>() ;
    
    // ... other code
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leak);
        
		 // NOTE: memory leaked
        sActivityLeaked.add(this) ;
    }
}
```

As you can see, we add MemoryLeakActivity instance to sActivityLeaked, but it's not deleted in anywhere, which causes memory leak. You can fix the problem by deleting related code. 

For other memory leaks, you can also use similar way to analyze the report and fix them.

<span id='mmat-config'></span>

## 三、mmat-config.json configuration

* `package`: App package to be tested
* `main_activity`: Class path of the App main activity(`exported=true` is to be added when register Activity in AndroidManifest.xml )
* `monkey_command`: Monkey command or Monkey shell path
* `enable_force_gc`: execute force gc before dumping hprof.
* `hprof_dir`:  root dir in android device to store hprof file dumped, the default dir is `/sdcard/`. If you can't access to `/sdcard`, you should change a new accessable dir, or you can't dump hprof. 
* `detect_leak_classes`: Class list of memory leak to be detected, including subclass of Actiivty and Fragment. If no other types class is needed to be detected, you don't have to modify it.
* `excluded_refs` :  Class list of memory leak to be excluded, for example Android system's memory leak. If the instance holded by WeakReference or SoftReference, it also needs to be excluded.
	* class : class path to be excluded
	* fields : certain fields in certain class to be excluded.
	* type: memory leak generated by static or instance field
* `bitmap_report` : bitmap report configuration
	* `max_report_count`: maximum bitmaps output in the report; If the value is -1, then no number limit.
	* `min_width` : minimum width of bitmap to be reported
	* `min_height`: minimum height of bitmap to be reported


**mmat-config.json demo:** 

```json
{
	"package": "com.example.mmat",
	"main_activity": "com.example.mmat.MainActivity",
	"monkey_command": "adb shell monkey -p com.example.mmat --ignore-crashes --ignore-timeouts --ignore-native-crashes --ignore-security-exceptions --pct-touch 40 --pct-motion 25 --pct-appswitch 10 --pct-rotation 5 -s 12358 -v -v -v --throttle 300 200",
	// "monkey_command": "/User/mrsimple/test_monkey.sh",   // monkey shell
	"enable_force_gc": true,
	"hprof_dir": "/data/local/tmp/",
	"detect_leak_classes": [
		"android.app.Activity", 
		"android.app.Fragment", 
		"android.support.v4.app.Fragment"
	],
	"excluded_refs": [
		{
			"class": "java.lang.ref.WeakReference",
			"fields": ["referent"],
			"type": "instance"
		},
		{
			"class": "java.lang.ref.SoftReference",
			"fields": ["referent"],
			"type": "instance"
		},
		{
			"class": "java.lang.ref.FinalizerReference",
			"fields": ["referent"],
			"type": "instance"
		},
		{
			"class": "android.arch.lifecycle.ReportFragment",
			"fields": [],
			"type": "static"
		}
	],
	"bitmap_report": {
		"max_report_count": 20,
		"min_width": 200,
		"min_height": 200
	}
}
```

## 四、AndroidManifest.xml application configuration demo

* add `android:debuggable="true"` in the application tag to ensure you can dump memory snapshot through adb shell in both debug and release mode. 
* add `android:exported="true"` and `android:launchMode="singleTask"` in MainActivity (App main page) 
    * `android:exported="true"`: make sure you can launch the App main page through adb shell
    * `android:launchMode="singleTask"` :  make sure when you launch the App main activity through adb shell, other Activities will be cleared, only the main activity is left in the Activity stack. That means all activities except for the maig activity should be destoryed.
 
For example:     

```xml
    <application
        android:debuggable="true"
            
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/AppTheme">
        
        <activity android:name=".MainActivity" android:exported="true" android:launchMode="singleTask">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
```

## License 

```
Copyright (C) 2019 Mr.Simple <simplecoder.h@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```