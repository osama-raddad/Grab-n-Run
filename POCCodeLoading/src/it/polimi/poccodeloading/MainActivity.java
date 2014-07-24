package it.polimi.poccodeloading;

import it.necst.grabnrun.SecureDexClassLoader;
import it.necst.grabnrun.SecureLoaderFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import dalvik.system.DexClassLoader;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

/**
 * This activity is the entry point of the application.
 * By interacting with the different elements in the list of buttons
 * it is possible to trigger different ways to retrieve external code
 * from either a remote or a local path during the application execution.
 * 
 * @author Luca Falsina
 *
 */
public class MainActivity extends Activity {

	// This array of strings contains the list of all the implemented
	// techniques for external code loading that should be visualized.
	public static final String techinquesToExecute[] = {	"DexClassLoader (.apk)", 
															"DexClassLoader (.jar)",
															"SecureDexClassLoader (.apk)", 
															"SecureDexClassLoader (.jar)",
															"CreatePackageContext"};
	
	// Auxiliary constants used for readability..
	private static final int DEX_CLASS_LOADER_APK = 0;
	private static final int DEX_CLASS_LOADER_JAR = 1;
	private static final int SECURE_DEX_CLASS_LOADER_APK = 2;
	private static final int SECURE_DEX_CLASS_LOADER_JAR = 3;
	private static final int CREATE_PACK_CTX = 4;
	
	// Unique identifier used for Log entries
	private static final String TAG_MAIN = MainActivity.class.getSimpleName();
	
	// Extra passed to the intent to trigger the new activity with correct test parameters
	private static final String IS_SECURE_LOADING_CHOSEN = "it.polimi.poccodeloading.IS_SECURE_LOADING_CHOSEN";
	
	// Used to validate dynamic code loading operations..
	private boolean effectiveDexClassLoader, effectiveSecureDexClassLoader;
	
	// String which represents the location of the apk container used for the test
	// and the name of the class to load dynamically..
	private String exampleAPKPath, classNameInAPK;
	
	// Used to visualize helper toast messages..
	private Handler toastHandler;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		effectiveDexClassLoader = false;
		effectiveSecureDexClassLoader = false;
		
		toastHandler = new Handler();
		
		//String exampleAPKPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/NasaDailyImage.apk";
		//String exampleAPKPath = Environment.getRootDirectory().getAbsolutePath() + "/ext_card/download/NasaDailyImage/NasaDailyImage.apk";
		exampleAPKPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/NasaDailyImage/NasaDailyImage.apk";
		
		classNameInAPK = "headfirstlab.nasadailyimage.NasaDailyImage";
		
		// The list view element is retrieved..
		ListView listView = (ListView) findViewById(R.id.listview);
		// Generate a dynamic list depending on the labels
		listView.setAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, techinquesToExecute));
				
		// Create a message handling object as an anonymous class.
		OnItemClickListener mMessageClickedHandler = new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						
				// Depending on the chosen button a different technique
				// is used..
				switch(position) {
			
					case DEX_CLASS_LOADER_APK:
						effectiveDexClassLoader = true;
						setUpDexClassLoader();
						effectiveDexClassLoader = false;
						Log.i(TAG_MAIN, "DexClassLoader from apk case should be started.");
						break;
				
					case DEX_CLASS_LOADER_JAR:
						Intent dexClassLoaderIntent = new Intent(MainActivity.this, DexClassSampleActivity.class);
						dexClassLoaderIntent.putExtra(IS_SECURE_LOADING_CHOSEN, false);
						startActivity(dexClassLoaderIntent);
						Log.i(TAG_MAIN, "DexClassLoader from jar case should be started.");
						break;
					
					case SECURE_DEX_CLASS_LOADER_APK:
						effectiveSecureDexClassLoader = true;
						setUpSecureDexClassLoader();
						effectiveSecureDexClassLoader = false;
						Log.i(TAG_MAIN, "SecureDexClassLoader from apk case should be started.");
						break;
				
					case SECURE_DEX_CLASS_LOADER_JAR:
						Intent secureDexClassLoaderIntent = new Intent(MainActivity.this, DexClassSampleActivity.class);
						secureDexClassLoaderIntent.putExtra(IS_SECURE_LOADING_CHOSEN, true);
						startActivity(secureDexClassLoaderIntent);
						Log.i(TAG_MAIN, "SecureDexClassLoader from jar case should be started.");
						break;
						
					case CREATE_PACK_CTX:
					
						break;
				
					default:
						Log.d(TAG_MAIN, "Invalid button choice!");
				}
			
			}

		};

		listView.setOnItemClickListener(mMessageClickedHandler);
		
	}

	protected void setUpSecureDexClassLoader() {
		
		// First check: this operation can only start after 
		// that the proper button has just been pressed..
		if (!effectiveSecureDexClassLoader) return;
				
		Log.i(TAG_MAIN, "Setting up SecureDexClassLoader..");
		
		// Create an instance of SecureLoaderFactory..
		// It needs as a parameter a Context object (an Activity is an extension of such a class..)
		SecureLoaderFactory mSecureLoaderFactory = new SecureLoaderFactory(this);
		
		SecureDexClassLoader mSecureDexClassLoader;
		
		// Aim: Retrieve NasaDailyImage apk securely
		// 1st Test: Fetch the certificate by reverting package name --> FAIL
		Log.i(TAG_MAIN, "1st Test: Fetch the certificate by reverting package name..");
		mSecureDexClassLoader = mSecureLoaderFactory.createDexClassLoader(exampleAPKPath, null, null, ClassLoader.getSystemClassLoader().getParent());		
		
		try {
			
			Class<?> loadedClass = mSecureDexClassLoader.loadClass(classNameInAPK);
			
			if (loadedClass != null) {
				Log.w(TAG_MAIN, "No class should be returned in this case!!");
			}
			else {
				Log.i(TAG_MAIN, "SecureDexClassLoader loads nothing since no certificate should have been found. SUCCESS!");
			}
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			Log.w(TAG_MAIN, "No class should be searched in this case!!");
		}
		
		// 2nd Test: Fetch the certificate by filling associative map 
		// between package name and certificate --> SUCCESS
		
		// Creating the apk paths list (you can mix between remote and local URL)..
		String listAPKPaths = 	Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/testApp.apk:" +
								exampleAPKPath + ":http://google.com/testApp2.apk";
		
		// Filling the associative map to link package names and certificates..
		Map<String, String> packageNamesToCertMap = new HashMap<String, String>();
		// 1st Location: valid remote certificate location
		packageNamesToCertMap.put("headfirstlab.nasadailyimage", "https://github.com/lukeFalsina/test/blob/master/test_cert.pem");
		// 2nd Location: inexistent certificate
		packageNamesToCertMap.put("it.polimi.example", "http://google.com/test_cert.pem");
		
		Log.i(TAG_MAIN, "2nd Test: Fetch the certificate by filling associative map..");
		mSecureDexClassLoader = mSecureLoaderFactory.createDexClassLoader(	listAPKPaths, 
																			null, 
																			packageNamesToCertMap, 
																			ClassLoader.getSystemClassLoader().getParent());
		
		try {
			Class<?> loadedClass = mSecureDexClassLoader.loadClass(classNameInAPK);
			
			if (loadedClass != null) {
				
				Activity NasaDailyActivity = (Activity) loadedClass.newInstance();
				
				Log.i(TAG_MAIN, "Found class: " + NasaDailyActivity.getLocalClassName() + 
								"; APK path: " + NasaDailyActivity.getPackageResourcePath() + "; SUCCESS!");
			} else {
				
				Log.w(TAG_MAIN, "This time the chosen class should pass the security checks!");
			}
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			Log.w(TAG_MAIN, "Class should be present in the provided path!!");
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * This method is used to set up and manage a DexClassLoader component in 
	 * order to retrieve a new activity from an .apk, which has been 
	 * already downloaded and installed on the mobile device.
	 * If everything works fine, it will instantiate the main activity of 
	 * this .apk.
	 * 
	 */
	protected void setUpDexClassLoader() {
		
		// First check: this operation can only start after 
		// that the proper button has just been pressed..
		if (!effectiveDexClassLoader) return;
		
		Log.i(TAG_MAIN, "Setting up DexClassLoader..");
		
		File dexOutputDir = getDir("dex", MODE_PRIVATE);
		DexClassLoader mDexClassLoader = new DexClassLoader(	exampleAPKPath, 
																dexOutputDir.getAbsolutePath(), 
																null, 
																ClassLoader.getSystemClassLoader().getParent());
		
		try {
			
			// Load NasaDailyImage Main Activity..
			Class<?> loadedClass = mDexClassLoader.loadClass(classNameInAPK);
			final Activity NasaDailyActivity = (Activity) loadedClass.newInstance();
			
			Log.i(TAG_MAIN, "Found class: " + loadedClass.getSimpleName() + "; APK path: " + exampleAPKPath.toString());
			
			toastHandler.post(new Runnable() {

				@Override
				public void run() {
					Toast.makeText(MainActivity.this,
							"DexClassLoader was successful! Found activity: " + NasaDailyActivity.getComponentName(),
							Toast.LENGTH_SHORT).show();
				}
				
			});
			
			// An intent is defined to start the new loaded activity.
			//Intent transitionIntent = new Intent(this, loadedClass);
			//startActivity(transitionIntent);
			//transitionIntent.setClassName("headfirstlab.nasadailyimage", "headfirstlab.nasadailyimage.NasaDailyImage");
			
		} catch (ClassNotFoundException e) {

			Log.e(TAG_MAIN, "Error: Class not found!");
			
			toastHandler.post(new Runnable() {

				@Override
				public void run() {
					Toast.makeText(MainActivity.this,
							"Error! No class found for DexClassLoader..",
							Toast.LENGTH_SHORT).show();
				}
				
			});
			
			e.printStackTrace();
		} catch (ActivityNotFoundException e) {
		
			Log.e(TAG_MAIN, "Error: Activity not found in the manifest!");
			
			toastHandler.post(new Runnable() {

				@Override
				public void run() {
					Toast.makeText(MainActivity.this,
							"Error! The activity found by DexClassLoader is not a legitimate one..",
							Toast.LENGTH_SHORT).show();
				}
				
			});
			
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
