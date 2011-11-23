if (typeof(AppMobiInit) != 'object')
    AppMobiInit = {};

/**
 * This represents the AppMobi API itself, and provides a global namespace for accessing
 * information about the state of AppMobi.
 * @class
 */
AppMobi = {
    queue: {
        ready: true,
        commands: [],
        timer: null
    },
    _constructors: [],
    jsVersion: '3.4.0'
};

/**
 * Boolean flag indicating if the AppMobi API is available and initialized.
 */
AppMobi.isnative = true;
AppMobi.isxdk = false;
AppMobi.available = false;
AppMobi.updateAvailable = false;
AppMobi.updateMessage = "";
AppMobi.app = "";
AppMobi.release = "";
AppMobi.webRoot = "";
AppMobi.cookies = {};
AppMobi.mediacache = [];
AppMobi.notifications = [];
AppMobi.picturelist = [];
AppMobi.recordinglist = [];
AppMobi.playingtrack = {};
AppMobi.oauthAvailable = false;

/**
 * Add an initialization function to a queue that ensures it will run and initialize
 * application constructors only once AppMobi has been initialized.
 * @param {Function} func The function callback you want run once AppMobi is initialized
 */
AppMobi.addConstructor = function(func) {
    var state = document.readyState;
    if (state == 'loaded' || state == 'complete')
        func();
    else
        AppMobi._constructors.push(func);
};

(function() {
	if(AppMobi.initialize==undefined) {
		AppMobi.initialize = function() {
		    var state = document.readyState;
			//console.log("In AppMobi.initialize, state:" + state);
			if (state == 'loaded' || state == 'complete') {
				//run constructors
				while (AppMobi._constructors.length > 0) {
					var constructor = AppMobi._constructors.shift();
					try {
						constructor();
					} catch(e) {
//						if (typeof(AppMobi.debug['log']) == 'function')
//							AppMobi.debug.log("Failed to run constructor: " + AppMobi.debug.processMessage(e));
//						else
//							alert("Failed to run constructor: " + e.message);
					}
				}

				// all constructors run, now fire the deviceready event
				var js = new String(AppMobiDevice.injectJSBeforeReady());
				eval(js.toString());
				var e = document.createEvent('Events');
				e.initEvent('appMobi.device.ready',true,true);
				document.dispatchEvent(e);
			} else {
			    setTimeout("AppMobi.initialize()", 100);
			}
		};
	}
	setTimeout("AppMobi.initialize()", 100);
})();

/**
 * Execute a AppMobi command in a queued fashion, to ensure commands do not
 * execute with any race conditions, and only run when AppMobi is ready to
 * recieve them.
 * @param {String} command Command to be run in AppMobi, e.g. "ClassName.method"
 * @param {String[]} [args] Zero or more arguments to pass to the method
 */
AppMobi.exec = function() {
	//compatibility placeholder on android - needed for iphone
};

/**
 * Internal function used to dispatch the request to AppMobi.  This needs to be implemented per-platform to
 * ensure that methods are called on the phone in a way appropriate for that device.
 * @private
 */
AppMobi.run_command = function() {
	//compatibility placeholder on android - needed for iphone
};

/**
 * This class contains acceleration information
 * @constructor
 * @param {Number} x The force applied by the device in the x-axis.
 * @param {Number} y The force applied by the device in the y-axis.
 * @param {Number} z The force applied by the device in the z-axis.
 * @param {boolean} doRotate If true, rotate axes based on device rotation.
 */
AppMobi.Acceleration = function(x, y, z, doRotate) {
	if(doRotate) {
		var orientation = AppMobi.device.orientation;
		if(orientation==0) {
		//portrait
		} else if (orientation==90) {
		//landscape left
			var temp = y, y = -x, x = temp;
		} else if (orientation==180) {
		//upside-down portrait
			x = -x, y = -y;
		} else if (orientation==-90) {
		//landscape right
			var temp = x, x = -y, y = temp;
		}
	}
	/**
	 * The force applied by the device in the x-axis.
	 */
	this.x = x;
	/**
	 * The force applied by the device in the y-axis.
	 */
	this.y = y;
	/**
	 * The force applied by the device in the z-axis.
	 */
	this.z = z;
	/**
	 * The time that the acceleration was obtained.
	 */
	this.timestamp = new Date().getTime();
}

// Define object for receiving acceleration values
AppMobi._accel = new AppMobi.Acceleration(0, 0, 0, false);

/**
 * This class specifies the options for requesting acceleration data.
 * @constructor
 */
AppMobi.AccelerationOptions = function() {
	this.frequency = 500;
	this.adjustForRotation = false;
}

/**
 * This class provides access to device accelerometer data.
 * @constructor
 */
AppMobi.Accelerometer = function() {
}

/**
 * Asynchronously acquires the current acceleration.
 * @param {Function} successCallback The function to call when the acceleration
 * data is available
 * @param {AccelerationOptions} options The options for getting the accelerometer data
 * such as timeout.
 */
AppMobi.Accelerometer.prototype.getCurrentAcceleration = function(successCallback, options) {
	// If the acceleration is available then call success
	// If the acceleration is not available then call error

	//validate options object
	var _options = new AppMobi.AccelerationOptions();
	if(typeof(options)=="object"){
		if(typeof(options.adjustForRotation)=="boolean") _options.adjustForRotation = options.adjustForRotation;
	}
	// Created for iPhone, Iphone passes back _accel obj litteral
	if (typeof successCallback == "function") {
		var accel = new AppMobi.Acceleration(AppMobi._accel.x,AppMobi._accel.y,AppMobi._accel.z, _options.adjustForRotation);
		successCallback(accel);
	}
}

/**
 * Asynchronously acquires the acceleration repeatedly at a given interval.
 * @param {Function} successCallback The function to call each time the acceleration
 * data is available
 * @param {AccelerationOptions} options The options for getting the accelerometer data
 * such as timeout.
 */

AppMobi.Accelerometer.prototype.watchAcceleration = function(successCallback, options) {
	//validate options object
	var _options = new AppMobi.AccelerationOptions();
	if(typeof(options)=="object"){
		var parsedFreq = parseInt(options.frequency);
		if(typeof(parsedFreq)=="number" && !isNaN(parsedFreq)) {
			_options.frequency = parsedFreq<25?25:parsedFreq;
		}
		if(typeof(options.adjustForRotation)=="boolean") _options.adjustForRotation = options.adjustForRotation;
	}
	AppMobiAccelerometer.start(_options.frequency);
	AppMobi.accelerometer.getCurrentAcceleration(successCallback, _options);
	return setInterval(function() {
		AppMobi.accelerometer.getCurrentAcceleration(successCallback, _options);
	}, _options.frequency);
}

/**
 * Clears the specified accelerometer watch.
 * @param {String} watchId The ID of the watch returned from #watchAcceleration.
 */
AppMobi.Accelerometer.prototype.clearWatch = function(watchId) {
	AppMobiAccelerometer.stop();
	clearInterval(watchId);
};

AppMobi.addConstructor(function() {
    if (typeof AppMobi.accelerometer == "undefined") AppMobi.accelerometer = new AppMobi.Accelerometer();
});

AppMobi.Advertising = function() {
};

AppMobi.Advertising.prototype.getAd = function(name, path, callbackId) {
	//path is the appconfig url
	//callbackId is a unique id that is used by the appmobi ad framework
	AppMobiAdvertising.getAd(name, path, callbackId);
};

AppMobi.addConstructor(function() {
    if (typeof AppMobi.advertising == "undefined") AppMobi.advertising = new AppMobi.Advertising();
});

/**
 * This class provides access to the appMobiPlayer
 * @constructor
 */
AppMobi.Player = function() {
};

AppMobi.Player.prototype.show = function() {
    AppMobiPlayer.show();
};

AppMobi.Player.prototype.hide = function() {
    AppMobiPlayer.hide();
};

AppMobi.Player.prototype.playPodcast = function(strPodcastURL) {
    AppMobiPlayer.playPodcast(strPodcastURL);
};

AppMobi.Player.prototype.startStation = function(strNetStationID, boolResumeMode, boolShowPlayer) {
    AppMobiPlayer.startStation(strNetStationID, boolResumeMode, boolShowPlayer);
};

AppMobi.Player.prototype.playSound = function(strRelativeFileURL) {
    AppMobiPlayer.playSound( strRelativeFileURL);
};

AppMobi.Player.prototype.loadSound = function(strRelativeFileURL) {
    AppMobiPlayer.loadSound( strRelativeFileURL);
};

AppMobi.Player.prototype.unloadSound = function(strRelativeFileURL) {
    AppMobiPlayer.unloadSound( strRelativeFileURL);
};

AppMobi.Player.prototype.startAudio = function(strRelativeFileURL) {
    AppMobiPlayer.startAudio(strRelativeFileURL);
};

AppMobi.Player.prototype.toggleAudio = function() {
    AppMobiPlayer.toggleAudio();
};

AppMobi.Player.prototype.stopAudio = function() {
    AppMobiPlayer.stopAudio();
};

AppMobi.Player.prototype.play = function() {
    AppMobiPlayer.play();
};

AppMobi.Player.prototype.pause = function() {
    AppMobiPlayer.pause();
};

AppMobi.Player.prototype.stop = function() {
    AppMobiPlayer.stop();
};

AppMobi.Player.prototype.volume = function(iPercentage) {
    AppMobiPlayer.volume(iPercentage);
};

AppMobi.Player.prototype.rewind = function() {
    AppMobiPlayer.rewind();
};

AppMobi.Player.prototype.ffwd = function() {
    AppMobiPlayer.ffwd();
};

AppMobi.Player.prototype.setColors = function(strBackColor, strFillColor, strDoneColor, strPlayColor) {
    AppMobiPlayer.setColors(strBackColor, strFillColor, strDoneColor, strPlayColor);
};

AppMobi.Player.prototype.setPosition = function(portraitX, portraitY, landscapeX, landscapeY) {
    AppMobiPlayer.setPosition(portraitX, portraitY, landscapeX, landscapeY);
};

AppMobi.Player.prototype.startShoutcast = function(strStationURL, boolShowPlayer) {
    AppMobiPlayer.startShoutcast(strStationURL, boolShowPlayer);
};

AppMobi.addConstructor(function() {
    if (typeof AppMobi.player == "undefined") AppMobi.player = new AppMobi.Player();
});

/**
 * This class provides access to the appMobiStats
 * @constructor
 */
AppMobi.Stats = function() {
};

AppMobi.Stats.prototype.logEvent = function(message) {
	throw(new Error("Error: AppMobi.stats.logEvent is deprecated, use AppMobi.analytics.logPageEvent."));
};

AppMobi.addConstructor(function() {
	if (typeof AppMobi.stats == "undefined") AppMobi.stats = new AppMobi.Stats();
});

/**
 * This class provides access to the appMobiAnalytics
 * @constructor
 */
AppMobi.Analytics = function() {
};

AppMobi.Analytics.prototype.logPageEvent = function(page, query, status, method, bytes, referrer) {
	if(page==undefined || page.length==0)
		throw(new Error("Error: AppMobi.analytics.logPageEvent, No page specified."));
	
	if( query == undefined || query.length == 0 ) query = "-";
	if( status == undefined || status.length == 0 ) status = "200";
	if( method == undefined || method.length == 0 ) method = "GET";
	if( bytes == undefined || bytes.length == 0 ) bytes = "0";
	if( referrer == undefined || referrer.length == 0 ) referrer = "index.html";
	
	AppMobiAnalytics.logPageEvent(page, query, status, method, bytes, referrer);
};

AppMobi.addConstructor(function() {
    if (typeof AppMobi.analytics == "undefined") AppMobi.analytics = new AppMobi.Analytics();
});

/**
 * This class provides access to the appMobiCalendar
 * @constructor
 */
AppMobi.Calendar = function() {
};

AppMobi.Calendar.prototype.addEvent = function(title, start, end) {
	AppMobiCalendar.addEvent(title, start, end);
};

AppMobi.addConstructor(function() {
	if (typeof AppMobi.calendar == "undefined") AppMobi.calendar = new AppMobi.Calendar();
});

/**
 * This class provides access to the appMobiFile
 * @constructor
 */
AppMobi.File = function() {
};

AppMobi.File.prototype.uploadToServer = function(localURL, uploadURL, foldername, mimetype, updateCallback) {
	if( (localURL == undefined || localURL=='') || (uploadURL == undefined || uploadURL=='') )
	{
		throw(new Error("Error: uploadToServer has the following required parameters: localURL, uploadURL."));
	}
	if(updateCallback != null && typeof(updateCallback)!="string") throw(new Error("Error: uploadToServer updateCallback parameter must be a string which is the name of a function"));
	if(mimetype == null) mimetype = "text/plain";
	
	AppMobiFile.uploadToServer(localURL, uploadURL, foldername, mimetype, updateCallback);
};

AppMobi.File.prototype.cancelUpload = function() {
	AppMobiFile.cancelUpload();
};

AppMobi.addConstructor(function() {
	if (typeof AppMobi.file == "undefined") AppMobi.file = new AppMobi.File();
});

/**
 * This class provides access to the appMobiSpeech
 * @constructor
 */
AppMobi.Speech = function() {
};

AppMobi.Speech.prototype.recognize = function(longPause, language) {
	if(longPause == null || longPause == undefined) longPause = false;
	if(language == null || language == undefined || language == '') language = "en_US";
	
	language = "en_US"; // for first version only allow US english
	AppMobiSpeech.recognize(longPause, language);
};

AppMobi.Speech.prototype.stopRecording = function() {
	AppMobiSpeech.stopRecording();
};

AppMobi.Speech.prototype.vocalize = function(text, voiceName, language) {
	if(text == undefined || text == null || text == '')
	{
		throw(new Error("Error: AppMobi.speech.vocalize has the following required parameters: text."));
	}
	if(voiceName == undefined || voiceName == null || voiceName == '') voiceName = "Samantha";
	
	language = "en_US"; // for first version only allow US english
	AppMobiSpeech.vocalize(text, voiceName, language);
};

AppMobi.Speech.prototype.cancel = function() {
		AppMobiSpeech.cancel();
};

AppMobi.addConstructor(function() {
   if (typeof AppMobi.speech == "undefined") AppMobi.speech = new AppMobi.Speech();
});

/**
 * This class provides access to the appMobiCamera
 * @constructor
 */
AppMobi.Camera = function() {
};

AppMobi.Camera.prototype.takePicture = function(quality, saveToLib, picType) {
	if(quality == undefined || quality == null)
		quality = 70; // default
	else if((quality<1) || (quality>100))
		throw(new Error("Error: AppMobi.camera.takePicture, quality must be between 1-100."));
	
	if(saveToLib == undefined || saveToLib == null) 
		saveToLib = true;
	
	if(typeof(picType) == "undefined" || picType == null) 
		picType = "jpg";
	else {
		if(typeof(picType) != "string")
			throw(new Error("Error: AppMobi.camera.takePicture, picType must be a string."));
		if((picType.toLowerCase() != "jpg") && (picType.toLowerCase() != "png"))
			throw(new Error("Error: AppMobi.camera.takePicture, picType must be 'jpg' or 'png'."));
	}
	AppMobiCamera.takePicture(quality, saveToLib, picType);
};

AppMobi.Camera.prototype.importPicture = function() {
	AppMobiCamera.importPicture();
};

AppMobi.Camera.prototype.deletePicture = function(picURL) {
	if(picURL == undefined || picURL == null) 
		throw(new Error("Error: AppMobi.camera.deletePicture, call with a picURL"));
	if(typeof(picURL) != "string")
		throw(new Error("Error: AppMobi.camera.deletePicture, picURL must be a string."));

	AppMobiCamera.deletePicture(picURL);
};

AppMobi.Camera.prototype.clearPictures = function() {
	AppMobiCamera.clearPictures();
};

AppMobi.Camera.prototype.getPictureList = function() {
	var list = [];
	for(var picture in AppMobi.picturelist) {
		list.push(AppMobi.picturelist[picture]);
	}
	return list;
}

AppMobi.Camera.prototype.getPictureURL = function(filename) {
	var localURL = undefined;
	var	found = false;
	for(var picture in AppMobi.picturelist) {
		if(filename == AppMobi.picturelist[picture]) {
			found=true;
			break;
		}
	}
	if(found)
		localURL = AppMobi.webRoot+'_pictures/'+filename;
	return localURL;
}

AppMobi.addConstructor(function() {
	if (typeof AppMobi.camera == "undefined") AppMobi.camera = new AppMobi.Camera();
});

/**
 * This class provides access to the appMobiAudio
 * @constructor
 */
AppMobi.Audio = function() {
	AppMobiAudio.initRecordingList();
};

AppMobi.Audio.prototype.startPlaying= function(recURL) {
    AppMobiAudio.startPlaying(recURL);
};

AppMobi.Audio.prototype.stopPlaying= function() {
    AppMobiAudio.stopPlaying();
};

AppMobi.Audio.prototype.pausePlaying= function() {
    AppMobiAudio.pausePlaying();
};

AppMobi.Audio.prototype.continuePlaying= function() {
    AppMobiAudio.continuePlaying();
};

AppMobi.Audio.prototype.startRecording= function(format, samplingRate, channels) {
    AppMobiAudio.startRecording(format, samplingRate, channels);
};

AppMobi.Audio.prototype.stopRecording= function() {
    AppMobiAudio.stopRecording();
};

AppMobi.Audio.prototype.pauseRecording= function() {
    AppMobiAudio.pauseRecording();
};

AppMobi.Audio.prototype.continueRecording= function() {
    AppMobiAudio.continueRecording();
};

AppMobi.Audio.prototype.deleteRecording= function(recURL) {
    AppMobiAudio.deleteRecording(recURL);
};

AppMobi.Audio.prototype.clearRecordings= function() {
    AppMobiAudio.clearRecordings();
};

AppMobi.Audio.prototype.getRecordingList = function() {
	var list = [];
	for(var recording in AppMobi.recordinglist) {
		list.push(AppMobi.recordinglist[recording]);
	}
	return list;
}

AppMobi.Audio.prototype.getRecordingURL = function(filename) {
	var localURL = undefined;
	var	found = false;
	for(var recording in AppMobi.recordinglist) {
		if(filename == AppMobi.recordinglist[recording]) {
			found=true;
			break;
		}
	}
	if(found)
		localURL = AppMobi.webRoot+'_recordings/'+filename;
	return localURL;
}

AppMobi.addConstructor(function() {
   if (typeof AppMobi.audio == "undefined") AppMobi.audio = new AppMobi.Audio();
});

/**
 * This class provides access to the appMobiPayments
 * @constructor
 */
AppMobi.Payments = function() {
};

AppMobi.Payments.prototype.getPaymentInfo = function(suc, err, sequence, price) {
	AppMobiPayments.getPaymentInfo(suc, err, sequence, price);
};

AppMobi.Payments.prototype.editPaymentInfo = function() {
	AppMobiPayments.editPaymentInfo();
};

AppMobi.Payments.prototype.buyApplication = function(app, rel, sequence, price) {
	AppMobiPayments.buyApplication(app, rel, sequence, price);
};

AppMobi.Payments.prototype.updateApplications = function() {
	AppMobiPayments.updateApplications();
};

AppMobi.addConstructor(function() {
	if (typeof AppMobi.payments == "undefined") AppMobi.payments = new AppMobi.Payments();
});

/**
 * This class provides access to the appMobiCache
 * @constructor
 */
AppMobi.Cache = function() {
	eval( "AppMobi.cookies = " + AppMobiCache.getCookies());
	eval( "AppMobi.mediacache = " + AppMobiCache.getMediaCache());
};

AppMobi.Cache.prototype.getCookie = function(name) {
	var cookie = undefined;
	try {
		//undelimit single quotes in value of cookie
		cookie = AppMobi.cookies[name].value.replace(/\\'/g, "'");
	} catch(e){};
	return cookie;
};

AppMobi.Cache.prototype.getCookieList = function() {
	var cookies = [];
	for(cookie in AppMobi.cookies) {
		cookies.push(cookie);
	}
	return cookies;
}

//if expires <0, the cookie does not expire
AppMobi.Cache.prototype.setCookie = function(name, value, days) {
	if(name==undefined || name.length==0)
		throw(new Error("Error: AppMobi.cache.setcookie, No cookie name specified."));
	if( name.indexOf('.') > -1 )
		throw(new Error("Error: AppMobi.cache.setcookie, No '.' allowed in cookie names."));
	var tester = "0123456789";
	if( tester.indexOf(name.charAt(0)) > -1 )
		throw(new Error("Error: AppMobi.cache.setcookie, No numbers as first character in cookie names."));
	
	if(typeof value == "undefined") {
		value = "";
	}

	//make sure value is a string
	value = String(value);
	
	AppMobi.cookies[name]={"value":value};
	AppMobiCache.setCookie(name, value, days);
};

AppMobi.Cache.prototype.removeCookie = function(name) {
	if(AppMobi.cookies&&AppMobi.cookies[name]) {
		delete AppMobi.cookies[name];
		AppMobiCache.removeCookie(name);
	}
}

AppMobi.Cache.prototype.clearAllCookies = function(name) {
	AppMobi.cookies = {};
	AppMobiCache.clearAllCookies();
}

AppMobi.Cache.prototype.getMediaCacheList = function() {
	var cache = [];
	for(var media in AppMobi.mediacache) {
		cache.push(AppMobi.mediacache[media]);
	}
	return cache;
}

AppMobi.Cache.prototype.getMediaCacheLocalURL = function(url) {
	var localURL = undefined;
	//check if the url is cached
	for(var file in AppMobi.mediacache) {
		if(url==AppMobi.mediacache[file]) {
			localURL = url.split('/');
			localURL = AppMobi.webRoot+'_mediacache/'+localURL[localURL.length-1];
			break;
		}
	}
	return localURL;
}

AppMobi.Cache.prototype.clearMediaCache = function() {
	AppMobiCache.clearMediaCache();
};

AppMobi.Cache.prototype.addToMediaCacheImpl = function(url, ID) {
	AppMobiCache.addToMediaCache(url, ID);
};

AppMobi.Cache.prototype.addToMediaCache = function(url) {
	AppMobi.cache.addToMediaCacheImpl(url, "");
};

AppMobi.Cache.prototype.addToMediaCacheExt = function(url, ID) {
	AppMobi.cache.addToMediaCacheImpl(url, ID);
};

AppMobi.Cache.prototype.removeFromMediaCache = function(url) {
	AppMobiCache.removeFromMediaCache(url);
};

AppMobi.addConstructor(function() {
    if (typeof AppMobi.cache == "undefined") AppMobi.cache = new AppMobi.Cache();
});

/**
 * This class provides access to the debugging console.
 * @constructor
 */
AppMobi.Debug = function() {
}

/**
 * Utility function for rendering and indenting strings, or serializing
 * objects to a string capable of being printed to the console.
 * @param {Object|String} message The string or object to convert to an indented string
 * @private
 */
AppMobi.Debug.prototype.processMessage = function(message) {
    if (typeof(message) != 'object') {
        return message;
    } else {
        /**
         * @function
         * @ignore
         */
        function indent(str) {
            return str.replace(/^/mg, "    ");
        }
        /**
         * @function
         * @ignore
         */
        function makeStructured(obj) {
            var str = "";
            for (var i in obj) {
                try {
                    if (typeof(obj[i]) == 'object') {
                        str += i + ":\n" + indent(makeStructured(obj[i])) + "\n";
                    } else {
                        str += i + " = " + indent(String(obj[i])).replace(/^    /, "") + "\n";
                    }
                } catch(e) {
                    str += i + " = EXCEPTION: " + e.message + "\n";
                }
            }
            return str;
        }
        return "Object:\n" + makeStructured(message);
    }
};

/**
 * Print a normal log message to the console
 * @param {Object|String} message Message or object to print to the console
 */
AppMobi.Debug.prototype.log = function(message) {
    if (AppMobi.available)
        AppMobiDebug.log(AppMobi.debug.processMessage(message), { logLevel: 'INFO' });
    else
        console.log(message);
};

/**
 * Print a warning message to the console
 * @param {Object|String} message Message or object to print to the console
 */
AppMobi.Debug.prototype.warn = function(message) {
    if (AppMobi.available)
        AppMobiDebug.log(AppMobi.debug.processMessage(message), { logLevel: 'WARN' });
    else
        console.error(message);
};

/**
 * Print an error message to the console
 * @param {Object|String} message Message or object to print to the console
 */
AppMobi.Debug.prototype.error = function(message) {
    if (AppMobi.available)
        AppMobiDebug.log(AppMobi.debug.processMessage(message), { logLevel: 'ERROR' });
    else
        console.error(message);
};

AppMobi.addConstructor(function() {
    if (typeof AppMobi.debug == "undefined") AppMobi.debug = new AppMobi.Debug();
});

/**
 * this represents the mobile device, and provides properties for inspecting the model, version, UUID of the
 * phone, etc.
 * @constructor
 */
AppMobi.Device = function() {
    this.available = AppMobi.available;
    this.platform = null;
    this.osversion = null;
    this.model = null;
    this.uuid = null;
    this.initialOrientation = null;
    this.appmobiversion = null;
    this.phonegapversion = null;
    this.orientation = null;
    this.connection = null;
    this.density = null;
    this.lastPlaying = null;
    this.hasAnalytics = null;
    this.hasCaching = null;
    this.hasStreaming = null;
    this.hasAdvertising = null;
    this.hasPush = null;
    this.hasPayments = null;
    this.hasUpdates = null;
    this.width = null;
    this.height = null;
    this.queryString = null;
    try {
        this.available = true;
        this.platform = AppMobiDevice.getPlatform();
        this.osversion = AppMobiDevice.getOSVersion();
        this.model = AppMobiDevice.getModel();
        this.uuid = AppMobiDevice.getUuid();
        this.initialOrientation = AppMobiDevice.getInitialOrientation();
        this.appmobiversion = AppMobiDevice.getVersion();
        this.phonegapversion = AppMobiDevice.getPGVersion();
        this.orientation = AppMobiDevice.getOrientation();
        this.connection = AppMobiDevice.getConnection();
        this.density = AppMobiDevice.getDisplayDensity();
        this.lastPlaying = AppMobiDevice.getPlayingStation();
        this.hasAnalytics = AppMobiDevice.getHasAnalytics();
        this.hasCaching = AppMobiDevice.getHasCaching();
        this.hasStreaming = AppMobiDevice.getHasStreaming();
        this.hasAdvertising = AppMobiDevice.getHasAdvertising();
	this.hasPush = AppMobiDevice.getHasPush();
	this.hasPayments = AppMobiDevice.getHasPayments();
	this.hasUpdates = AppMobiDevice.getHasUpdates();
	this.queryString = AppMobiDevice.getQueryString();
        this.width = AppMobiDevice.getDisplayWidth();
        this.height = AppMobiDevice.getDisplayHeight();
    } catch(e) {
        this.available = false;
    }
    
    //setup polling for keyboard visibility
    this.isSoftKeyboardShowing = false;
    this.checkKeyboard = function() {
    	if(this.isSoftKeyboardShowing!=AppMobiDevice.isSoftKeyboardShowing()) {
    		this.isSoftKeyboardShowing=AppMobiDevice.isSoftKeyboardShowing();
    		var e = document.createEvent('Events');
    		e.initEvent((this.isSoftKeyboardShowing?'appMobi.device.keyboard.show':'appMobi.device.keyboard.hide'),true,true);
    		document.dispatchEvent(e);
    	}
    	setTimeout("AppMobi.device.checkKeyboard()", 250);
    };
    this.checkKeyboard();
}

/**
 * This class specifies the attributes for getRemoteDataExt.
 * @constructor
 */
AppMobi.Device.RemoteDataParameters = function() {
	this.url = "";
	this.id = "";
	this.method = "GET";
	this.body = "";
	this.headers = "";
};

AppMobi.Device.RemoteDataParameters.prototype.addHeader = function(name, value) {
	if(typeof name != "string" || name.length==0) {
		throw(new Error("Error: the name parameter must be of type 'string' and must be of length > 0 for RemoteDataParameters"));
	}
	
	if(typeof value != "string") {

		if(typeof value == "undefined") {
			//convert undefined to zero-length string
			value = "";
		} else {
			//convert other non-strings (function, number, object, etc.) to string representation
			value = String(value);
		}
		
	}

	this.headers += name.length +"~" + name + value.length +"~" + value;
};

AppMobi.Device.prototype.managePower = function(shouldStayOn, onlyWhenPluggedIn) {
	AppMobiDevice.managePower(shouldStayOn, onlyWhenPluggedIn);
};

AppMobi.Device.prototype.setAutoRotate = function(shouldAutoRotate) {
	AppMobiDevice.setAutoRotate(shouldAutoRotate);
};

AppMobi.Device.prototype.setRotateOrientation = function(orientation) {
	//orientation should be 'portrait' or 'landscape'
	AppMobiDevice.setRotateOrientation(orientation);
};

AppMobi.Device.prototype.updateConnection = function() {
	AppMobiDevice.updateConnection();
};

AppMobi.Device.prototype.setBasicAuthentication = function(host, username, password) {
	AppMobiDevice.setBasicAuthentication(host, username, password);
};

AppMobi.Device.prototype.addVirtualPage = function() {
	AppMobiDevice.addVirtualPage();
};

AppMobi.Device.prototype.removeVirtualPage = function() {
	AppMobiDevice.removeVirtualPage();
};

AppMobi.Device.prototype.addMenuItem = function(text, callback) {
	AppMobiDevice.addMenuItem(text, callback);
};

AppMobi.Device.prototype.registerLibrary = function(strDelegateName) {
	AppMobiDevice.registerLibrary(strDelegateName);
};

AppMobi.Device.prototype.launchExternal = function(strURL) {
	AppMobiDevice.launchExternal(strURL);
};

AppMobi.Device.prototype.showRemoteSite = function(strURL, closeX, closeY, closeWidth, closeHeight) {
	AppMobi.device.showRemoteSiteExt(strURL, closeX, closeY, closeX, closeY, closeWidth, closeHeight);
};

AppMobi.Device.prototype.showRemoteSiteExt = function(strURL, closePortX, closePortY, closeLandX, closeLandY, closeWidth, closeHeight) {
	AppMobiDevice.showRemoteSite(strURL, closePortX, closePortY, closeLandX, closeLandY, closeWidth, closeHeight);
};

AppMobi.Device.prototype.closeRemoteSite = function() {
	AppMobiDevice.closeRemoteSite();
};

//internal only
AppMobi.adBlockBeforeUnloadListener = function(e) {
	AppMobiDevice.stopLoading();
}

AppMobi.Device.prototype.blockRemotePages = function(shouldblock, whitelist) {
	try {
		if(shouldblock) {
			AppMobiDevice.setBlockedPagesWhitelist(whitelist);
			window.addEventListener("beforeunload", AppMobi.adBlockBeforeUnloadListener, true);
		} else {
			window.removeEventListener("beforeunload", AppMobi.adBlockBeforeUnloadListener, true);
		}
	}
	catch(e) {
		console.log(e);
	}
};

AppMobi.Device.prototype.scanBarcode = function() {
	AppMobiDevice.scanBarcode();
};

/**
 * Set the current orientation of the phone.  This is called from the device automatically.
 *
 * When the orientation is changed, the DOMEvent \c orientationChanged is dispatched against
 * the document element.  The event has the property \c orieentation which can be used to retrieve
 * the device's current orientation.
 *
 * @param {Number} orientation The orientation to be set
 */
AppMobi.Device.prototype.setOrientation = function(orientation) {
	AppMobi.device.orientation = orientation;

	var e = document.createEvent('Events');
	e.initEvent('appMobi.device.orientation.change', true, true);
	e.orientation = orientation;
	document.dispatchEvent(e);
};

AppMobi.Device.prototype.getRemoteDataImpl = function(requestUrl, requestMethod, requestBody, successCallback, errorCallback, id, hasId) {
	//validate parameters
	if(
		 (requestUrl == undefined || requestUrl=='') ||
		 (requestMethod == undefined || requestMethod=='' || (requestMethod.toUpperCase()!='GET' && requestMethod.toUpperCase()!='POST')) ||
		 (successCallback == undefined || successCallback=='') ||
		 (errorCallback == undefined || errorCallback=='')
		 ) {
		throw(new Error("Error: getRemoteData has the following required parameters: requestUrl, requestMethod, requestBody, successCallback, errorCallback.  requestMethod must be either GET or POST.  requestBody is ignored for GET requests."));
	}

	if(typeof(successCallback)!="string") throw(new Error("Error: getRemoteData successCallback parameter must be a string which is the name of a function"));
	if(typeof(errorCallback)!="string") throw(new Error("Error: getRemoteData errorCallback parameter must be a string which is the name of a function"));

	if(requestBody == undefined) requestBody="";
	
	AppMobiDevice.getRemoteData(requestUrl, requestMethod, requestBody, successCallback, errorCallback, id, hasId);
};

AppMobi.Device.prototype.getRemoteDataWithId = function(requestUrl, requestMethod, requestBody, successCallback, errorCallback, id) {
	AppMobi.device.getRemoteDataImpl(requestUrl, requestMethod, requestBody, successCallback, errorCallback, id, true);
};

AppMobi.Device.prototype.getRemoteDataWithID = function(requestUrl, requestMethod, requestBody, successCallback, errorCallback, id) {
	AppMobi.device.getRemoteDataImpl(requestUrl, requestMethod, requestBody, successCallback, errorCallback, id, true);
};

AppMobi.Device.prototype.getRemoteData = function(requestUrl, requestMethod, requestBody, successCallback, errorCallback) {
	AppMobi.device.getRemoteDataImpl(requestUrl, requestMethod, requestBody, successCallback, errorCallback, "", false);
};

AppMobi.Device.prototype.getRemoteDataExt = function(parameters) {
	if( parameters == undefined )
	{
		throw(new Error("Error: AppMobi.device.getRemoteDataExt: parameters is required."));
	}

	if( parameters.hasOwnProperty("url") == false || parameters.hasOwnProperty("id") == false || parameters.hasOwnProperty("method") == false 
	   || parameters.hasOwnProperty("body") == false || parameters.hasOwnProperty("headers") == false )
	{
		throw(new Error("Error: AppMobi.device.getRemoteDataExt: invalid parameters object. Initialize using 'new AppMobi.Device.RemoteDataParameters'."));
	}
	
	if( parameters.url == undefined || parameters.url=='' )
	{
		throw(new Error("Error: AppMobi.device.getRemoteDataExt requires a url property."));
	}

	if( parameters.method == undefined || (parameters.method.toUpperCase()!='GET' && parameters.method.toUpperCase()!='POST') )
	{
		throw(new Error("Error: AppMobi.device.getRemoteDataExt requires a method property of GET or POST. body is ignored for GET requests."));
	}
	
	AppMobiDevice.getRemoteDataExt(parameters.url, parameters.id, parameters.method, parameters.body, parameters.headers);
};

AppMobi.Device.prototype.installUpdate = function() {
	AppMobiDevice.installUpdate();
};

AppMobi.Device.prototype.hideSplashScreen = function() {
	AppMobiDevice.hideSplashScreen();
};

AppMobi.addConstructor(function() {
    if (typeof AppMobi.device == "undefined") AppMobi.device = new AppMobi.Device();
});

/**
 * This class provides access to the appMobiGeolocation
 * @constructor
 */
AppMobi.Geolocation = function() {
	this.watchIDs = new Array();
	this.successCBs = new Array();
	this.errorCBs = new Array();
	this.pollID = -1;
};

AppMobi.Geolocation.Coords = function(latitude, longitude, altitude, accuracy, altitudeAccuracy, heading, speed) {
	this.latitude = latitude;
	this.longitude = longitude;
	this.altitude = altitude;
	this.accuracy = accuracy;
	this.altitudeAccuracy = altitudeAccuracy;
	this.heading = heading;
	this.speed = speed;
};

AppMobi.Geolocation.Position = function(coords, timestamp) {
	this.coords = coords;
	this.timestamp = timestamp;
};

AppMobi.Geolocation.prototype.poll = function() {
	var i, to = 0, len = this.watchIDs.length;
	//AppMobiGeolocation.printMessage("poll: watchIDs.length = " + len);
	for (i = 0; i < len; ++i) {
		var loc = AppMobiGeolocation.pollLocation(this.watchIDs[i]);
		if (loc.length()) {	
			loc = new String(loc);	// Convert to actual string.
			var vals = loc.split(",");
			var once = vals[0];
			//AppMobiGeolocation.printMessage("poll: vals.length = " + vals.length + ", once = " + once + ", vals = " + vals);
			this.successCB(vals[1],vals[2],vals[3],vals[4],vals[5],vals[6],vals[7],vals[8], vals[9]);
			if (once == 0)
				this.watchIDs[to++] = this.watchIDs[i];
		} else
			this.watchIDs[to++] = this.watchIDs[i];
	}
	if (to < len)
		this.watchIDs.splice(to);
};

AppMobi.Geolocation.prototype.getSuccessId = function(successCallback) {
	var i, len = this.successCBs.length;
	for (i = 0; i < len; ++i) {
		if (this.successCBs[i] == successCallback)
			return i;
	}
	len = this.successCBs.push(successCallback);
	return len - 1;
};

AppMobi.Geolocation.prototype.getErrorId = function(errorCallback) {
	var i, len = this.errorCBs.length;
	for (i = 0; i < len; ++i) {
		if (this.errorCBs[i] == errorCallback)
			return i;
	}
	len = this.errorCBs.push(errorCallback);
	return len - 1;
};

AppMobi.Geolocation.prototype.successCB = function(ID, latitude, longitude, altitude, accuracy,
			altitudeAccuracy, heading, speed, timestamp) {
	var fun = this.successCBs[ID];
	var coords = new AppMobi.Geolocation.Coords(latitude, longitude, altitude, accuracy, altitudeAccuracy, heading, speed);
	var p = new AppMobi.Geolocation.Position(coords, timestamp);
	fun(p);
};

AppMobi.Geolocation.prototype.errorCB = function(ID) {
	var fun = this.errorCBs[ID];
	fun();
};

AppMobi.Geolocation.prototype.getCurrentPosition = function(successCallback, errorCallback, options) {
	if( (successCallback == undefined || successCallback==''))
	{
		throw(new Error("Error: getCurrentPosition has the following required parameters: successCallback."));
	}
	var successID = this.getSuccessId(successCallback);
	var errorID = errorCallback == undefined ? -1 : this.getErrorId(errorCallback);
	var enableHighAccuracy = true;
	var maximumAge = -1;
	if (options != undefined) {
		if (options.maximumAge != undefined)
			maximumAge = options.maximumAge;
		if (options.enableHighAccuracy != undefined)
			enableHighAccuracy = options.enableHighAccuracy;
	}
	var id = AppMobiGeolocation.getCurrentPosition(successID, errorID, maximumAge, enableHighAccuracy);
	//AppMobiGeolocation.printMessage("getCurrentPosition: " + id + ", pollID = " + this.pollID);
	this.watchIDs.push(id);
	if (this.pollID == -1)
		this.pollID = setInterval("AppMobi.geolocation.poll()", 200);	// Poll every 200 msecs.
};

AppMobi.Geolocation.prototype.watchPosition = function(successCallback, errorCallback, options) {
	if( (successCallback == undefined || successCallback==''))
	{
		throw(new Error("Error: watchPosition has the following required parameters: successCallback."));
	}
	var successID = this.getSuccessId(successCallback);
	var errorID = errorCallback == undefined ? -1 : this.getErrorId(errorCallback);
	var freq = 10000;
	var enableHighAccuracy = true;
	var maximumAge = -1;
	if (options != undefined) {
		if (options.timeout != undefined)
			freq = options.timeout;
		if (options.maximumAge != undefined)
			maximumAge = options.maximumAge;
		if (options.enableHighAccuracy != undefined)
		enableHighAccuracy = options.enableHighAccuracy;
	}
	var id = AppMobiGeolocation.watchPosition(successID, errorID, freq, maximumAge, enableHighAccuracy);
	//AppMobiGeolocation.printMessage("watchPosition: " + id + ", pollID = " + this.pollID);
	this.watchIDs.push(id);
	if (this.pollID == -1) {
		//AppMobiGeolocation.printMessage("About to schedule poll()");
		this.pollID = setInterval("AppMobi.geolocation.poll()", 200);	// Poll every 200 msecs.
	}
	return id;
};

AppMobi.Geolocation.prototype.clearWatch = function(id) {
	AppMobiGeolocation.clearWatch(id);
	var index = this.watchIDs.indexOf(id);
	if (index != -1) {
		this.watchIds.splice(index, 1);
		if (this.watchIds.length == 0 && this.pollID != -1) {
			clearInterval(this.pollID);
			this.pollID = -1;
		}
	}
};

// Use OLD method until we finish final testing on new object
//AppMobi.addConstructor(function() {
//	if (typeof AppMobi.geolocation == "undefined") AppMobi.geolocation = new AppMobi.Geolocation();
//});

/* ++++++THE OLD METHOD.  To be replaced by AppMobi.Geolocation.*/
AppMobi.addConstructor(function() {
    if (typeof AppMobi.geolocation == "undefined") AppMobi.geolocation = navigator.geolocation;
});

/**
 * This class provides access to notifications on the device.
 */
AppMobi.Notification = function() {
	eval(AppMobiNotification.getNotificationsString());
}

/*
 This class specifies the attributes for push users.
 * @constructor
 */
AppMobi.Notification.PushUserAttributes = function() {
	this.s1 = "";
	this.s2 = "";
	this.s3 = "";
	this.s4 = "";
	this.s5 = "";
	this.s6 = "";
	this.n1 = "";
	this.n2 = "";
	this.n3 = "";
	this.n4 = "";
};

/**
 * Open a native alert dialog, with a customizable title and button text.
 * @param {String} message Message to print in the body of the alert
 * @param {String} [title="Alert"] Title of the alert dialog (default: Alert)
 * @param {String} [buttonLabel="OK"] Label of the close button (default: OK)
 */
AppMobi.Notification.prototype.alert = function(message, title, button) {
    if (AppMobi.available)
        AppMobiNotification.alert(message, title, button);
    else
        alert(message);
};

/**
 * Causes the device to vibrate.
 */
AppMobi.Notification.prototype.vibrate = function() {
    AppMobiNotification.vibrate();
};

/**
 * Causes the device to beep.
 * @param {Integer} count The number of beeps.
 */
AppMobi.Notification.prototype.beep = function(count) {
    AppMobiNotification.beep(count);
};

AppMobi.Notification.prototype.showBusyIndicator = function() {
    AppMobiNotification.showBusyIndicator();
};

AppMobi.Notification.prototype.hideBusyIndicator = function() {
    AppMobiNotification.hideBusyIndicator();
};

AppMobi.Notification.prototype.getNotificationList = function() {
	var notify = [];
	for(var note in AppMobi.notifications) {
		notify.push(AppMobi.notifications[note].id);
	}
	return notify;
};

AppMobi.Notification.prototype.getNotificationData = function(id) {
	var local = null;
	for(var note in AppMobi.notifications) {
		if(id==AppMobi.notifications[note].id) {
			local = {};
			local.id = AppMobi.notifications[note].id;
			local.msg = AppMobi.notifications[note].msg;
			local.data = AppMobi.notifications[note].data;
			local.userkey = AppMobi.notifications[note].userkey;
			local.richurl = AppMobi.notifications[note].richurl;
			local.richhtml = AppMobi.notifications[note].richhtml;
			local.isRich = AppMobi.notifications[note].isRich;
			break;
		}
	}
	return local;
};

AppMobi.Notification.prototype.checkPushUser = function(userID, password) {
	if( userID   == undefined || userID   == "" ||
		 password == undefined || password == "" )
	{
		throw(new Error("Error: AppMobi.notification.checkPushUser, No user or password specified."));
	}
	
	AppMobiNotification.checkPushUser(userID, password);
};

AppMobi.Notification.prototype.addPushUser = function(userID, password, email) {
	if( userID   == undefined || userID   == "" || userID.indexOf(' ')!=-1 || userID.indexOf('.')!=-1 || 
		 password == undefined || password == "" || password.indexOf(' ')!=-1 || password.indexOf('.')!=-1 ||
		 email    == undefined || email    == "" )
	{
		throw(new Error("Error: AppMobi.notification.addPushUser: User, email and password are required.  The space (' ') and dot ('.') characters are not allowed in user and password."));
	}

	AppMobiNotification.addPushUser(userID, password, email);
};

AppMobi.Notification.prototype.editPushUser = function(newEmail, newPassword) {
	if( ( newEmail == undefined || newEmail    == "" ) &&
			( newPassword == undefined || newPassword == "" ) )
	{
		throw(new Error("Error: AppMobi.notification.editPushUser, No new value (email or password) or password specified."));
	}

	AppMobiNotification.editPushUser(newEmail, newPassword);
};

AppMobi.Notification.prototype.deletePushUser = function() {
	AppMobiNotification.deletePushUser();
};

AppMobi.Notification.prototype.sendPushUserPass = function() {
	AppMobiNotification.sendPushUserPass();
};

AppMobi.Notification.prototype.setPushUserAttributes = function(attributes) {
	if( attributes == undefined )
	{
		throw(new Error("Error: AppMobi.notification.setPushUserAttributes: attributes is required."));
	}

	if( attributes.hasOwnProperty("s1") == false || attributes.hasOwnProperty("s2") == false || attributes.hasOwnProperty("s3") == false || attributes.hasOwnProperty("s4") == false 
			|| attributes.hasOwnProperty("s5") == false || attributes.hasOwnProperty("s6") == false || attributes.hasOwnProperty("n1") == false
			 || attributes.hasOwnProperty("n2") == false || attributes.hasOwnProperty("n3") == false || attributes.hasOwnProperty("n4") == false )
	{
		throw(new Error("Error: AppMobi.notification.setPushUserAttributes: invalid attributes parameter specified. Initialize using 'new AppMobi.Notification.PushUserAttributes'."));
	}

	if( (Number(attributes.n1) == NaN) || (Number(attributes.n2) == NaN) || 
			(Number(attributes.n3) == NaN) || (Number(attributes.n4) == NaN) )
	{
		throw(new Error("Error: AppMobi.notification.setPushUserAttributes: attributes n1,n2,n3,n4 must be numbers."));
	}

	var parsedAttributes = "";
	for(var prop in attributes) {
		if( (prop=="s1"||prop=="s2"||prop=="s3"||prop=="s4"||prop=="s5"||prop=="s6"||prop=="n1"||prop=="n2"||prop=="n3"||prop=="n4") && attributes[prop] != "") {
			parsedAttributes += "[";
			parsedAttributes += prop;
			parsedAttributes += "=";
			parsedAttributes += escape(attributes[prop]);
			parsedAttributes += "]";
		}
	}
	AppMobiNotification.setPushUserAttributes(parsedAttributes);
};

AppMobi.Notification.prototype.findPushUser = function(userID, email) {
	AppMobiNotification.findPushUser(userID, email);
};

AppMobi.Notification.prototype.refreshPushNotifications = function() {
	AppMobiNotification.refreshPushNotifications();
};

AppMobi.Notification.prototype.readPushNotifications = function(notificationIDs) {
	if( notificationIDs == undefined || notificationIDs == "")
	{
		throw(new Error("Error: AppMobi.notification.readPushNotifications, No notificationIDs specified."));
	}

	AppMobiNotification.readPushNotifications(notificationIDs);
};

AppMobi.Notification.prototype.deletePushNotifications = function(notificationIDs) {
	if( notificationIDs == undefined || notificationIDs == "")
	{
		throw(new Error("Error: AppMobi.notification.deletePushNotifications, No notificationIDs specified."));
	}

	AppMobiNotification.readPushNotifications(notificationIDs);
};

AppMobi.Notification.prototype.sendPushNotification = function(userID, message, data) {
	if( userID  == undefined || userID  == "" ||
		message == undefined || message == "" || 
		data    == undefined || data    == "" )
	{
		throw(new Error("Error: AppMobi.notification.sendPushNotification, No user, message or data specified."));
	}
	
	if(message.length>160) throw(new Error("Error: AppMobi.notification.sendPushNotification, message cannot exceed 160 characters in length."));
	if(data.length>160) throw(new Error("Error: AppMobi.notification.sendPushNotification, data cannot exceed 160 characters in length."));

	AppMobiNotification.sendPushNotification(userID, message, data);
};

AppMobi.Notification.prototype.showRichPushViewer = function(notificationID, closePortX, closePortY, closeLandX, closeLandY, closeWidth, closeHeight) {
	if( notificationID == undefined || notificationID  == ""){
		throw(new Error("Error: AppMobi.notification.showRichPushViewer, No notification ID specified."));
	}
	
	var notification = AppMobi.notification.getNotificationData( notificationID );
	if( notification == null || ( notification.richurl == "" && notification.richhtml == "" ) ){
		throw(new Error("Error: AppMobi.notification.showRichPushViewer, This notification ID is not a rich message."));
	}
	
	AppMobiNotification.showRichPushViewer(notificationID, closePortX, closePortY, closeLandX, closeLandY, closeWidth, closeHeight);
};

AppMobi.Notification.prototype.closeRichPushViewer = function() {
	AppMobiNotification.closeRichPushViewer();
};

AppMobi.addConstructor(function() {
    if (typeof AppMobi.notification == "undefined") AppMobi.notification = new AppMobi.Notification();
});

/**
 * This class provides access to the device display.
 * @constructor
 */
AppMobi.Display = function() {
    this.viewport = {};
    this.oldviewport = {};
}

/**
//turns on appmobi managed viewport
 * @param {int} portraitWidthInPx
 * @param {int} landscapeWidthInPx
 */
AppMobi.Display.prototype.useViewport = function(portraitWidthInPx, landscapeWidthInPx) {
    AppMobi.display.viewport.portraitWidth = parseInt(portraitWidthInPx);
    AppMobi.display.viewport.landscapeWidth = parseInt(landscapeWidthInPx);
    if(isNaN(AppMobi.display.viewport.portraitWidth)||isNaN(AppMobi.display.viewport.landscapeWidth)) return;
    document.addEventListener('appMobi.device.orientation.change', AppMobi.display.viewportOrientationListener, false);
    AppMobi.display.updateViewportOrientation(AppMobi.device.orientation);
}

AppMobi.Display.prototype.updateViewportContent = function(content) {
    //get reference to head
    var head, heads = document.getElementsByTagName('head');
    if(heads.length>0) head = heads[0];
    else return;
    //remove any viewport meta tags
    var metas = document.getElementsByTagName('meta');
    for(var i=0;i<metas.length;i++) {
        if(metas[i].name=='viewport') try {head.removeChild(metas[i]);} catch(e){}
    }
    //add the new viewport meta tag
    var viewport = document.createElement('meta');
    viewport.setAttribute('name', 'viewport');
    viewport.setAttribute('id', 'viewport');
    viewport.setAttribute('content', content);
    head.appendChild(viewport);
}

AppMobi.Display.prototype.updateViewportOrientation = function(orientation) {
    var width, deviceWidth;
    if(orientation==0||orientation==180) {
        width=AppMobi.display.viewport.portraitWidth;
    } else {
        width=AppMobi.display.viewport.landscapeWidth;
    }
    deviceWidth = AppMobi.device.width;
    var osver = parseFloat(AppMobi.device.osversion);
    var scale;
    if(osver<2.2) {
    	scale = (screen.width)/(width*AppMobi.device.density);//needed for any-density = false, otherwise (screen.width)/width
    } else {
    	scale = deviceWidth/width;
    }

    if(AppMobiDevice.hasHTCUndocumentedMethods()) {
    	scale = (Math.round(scale*100))/100;
	    AppMobiDisplay.forceScale(scale);
    } else {
    	AppMobi.display.updateViewportContent('minimum-scale='+scale+',maximum-scale='+scale);//if any-density=true, prepend with target-densitydpi=device-dpi,
    }
    //AppMobi.debug.log("width:"+width+",deviceWidth:"+deviceWidth+",screen.width:"+screen.width+",AppMobi.device.density:"+AppMobi.device.density+",osver:"+osver+",scale:"+scale);
}

AppMobi.Display.prototype.viewportOrientationListener = function(e){
    AppMobi.display.updateViewportOrientation(AppMobi.device.orientation);
}

AppMobi.Display.prototype.startAR = function() {
    AppMobiDisplay.startAR();
};

AppMobi.Display.prototype.stopAR = function() {
    AppMobiDisplay.stopAR();
};

AppMobi.Display.prototype.switchViewport = function(currentDoc, portraitWidthInPx, landscapeWidthInPx) {
	this.oldviewport.portraitWidth = this.viewport.portraitWidth;
	this.oldviewport.landscapeWidth = this.viewport.landscapeWidth;
	this.useViewport(portraitWidthInPx, landscapeWidthInPx);
}

AppMobi.Display.prototype.revertViewport = function() {
	this.useViewport(this.oldviewport.portraitWidth, this.oldviewport.landscapeWidth);
}

AppMobi.addConstructor(function() {
    if (typeof AppMobi.display == "undefined") AppMobi.display = new AppMobi.Display();
});

/**
 * This class provides access to oauth.
 * @constructor
 */
AppMobi.OAuth = function() {
}

/**
 * This class specifies the attributes for getRemoteDataExt.
 * @constructor
 */
AppMobi.OAuth.ProtectedDataParameters = function() {
	this.service = "";
	this.url = "";
	this.id = "";
	this.method = "GET";
	this.body = "";
	this.headers = "";
};

AppMobi.OAuth.ProtectedDataParameters.prototype.addHeader = function(name, value) {
	if(typeof name != "string" || name.length==0) {
		throw(new Error("Error: the name parameter must be of type 'string' and must be of length > 0 for ProtectedDataParameters"));
	}
	
	if(typeof value != "string") {		
		if(typeof value == "undefined") {
			//convert undefined to zero-length string
			value = "";
		} else {
			//convert other non-strings (function, number, object, etc.) to string representation
			value = String(value);
		}		
	}
	
	this.headers += name.length +"~" + name + value.length +"~" + value;
};

AppMobi.OAuth.prototype.unauthorizeService = function(service) {
	AppMobiOAuth.unauthorizeService(service);
}

AppMobi.OAuth.prototype.getProtectedData = function(parameters) {
	if( parameters == undefined )
	{
		throw(new Error("Error: AppMobi.oauth.getProtectedData: parameters is required."));
	}
	
	if( parameters.hasOwnProperty("service") == false || parameters.hasOwnProperty("url") == false || parameters.hasOwnProperty("id") == false 
	    || parameters.hasOwnProperty("method") == false || parameters.hasOwnProperty("body") == false || parameters.hasOwnProperty("headers") == false )
	{
		throw(new Error("Error: AppMobi.oauth.getProtectedData: invalid parameters object. Initialize using 'new AppMobi.Device.RemoteDataParameters'."));
	}
	
	if( parameters.service == undefined || parameters.service=='' )
	{
		throw(new Error("Error: AppMobi.oauth.getProtectedData requires a service property."));
	}
	
	if( parameters.url == undefined || parameters.url=='' )
	{
		throw(new Error("Error: AppMobi.oauth.getProtectedData requires a url property."));
	}
	
	if( parameters.method == undefined || (parameters.method.toUpperCase()!='GET' && parameters.method.toUpperCase()!='POST') )
	{
		throw(new Error("Error: AppMobi.oauth.getProtectedData requires a method property of GET or POST. body is ignored for GET requests."));
	}
	
	AppMobiOAuth.getProtectedData(parameters.service, parameters.url, parameters.id, parameters.method, parameters.body, parameters.headers);
};

AppMobi.addConstructor(function() {
    if (typeof AppMobi.oauth == "undefined") AppMobi.oauth = new AppMobi.OAuth();
});

function getAppMobiObject()
{
	if( typeof AppMobi == "undefined" )
		return null;
	return AppMobi;
}

function getAppMobiDocument()
{
	return document;
}