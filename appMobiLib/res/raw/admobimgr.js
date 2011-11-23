
function onAdLoaded(ev)
{
	if( ev.identifier == uniqueId && ev.success == true )
	{
		document.write('<iframe src="' + ev.path + '" scrolling="no" style="border:0px solid #ffffff;overflow:hidden;position:absolute;left:0px;top:0px;width:300px;height:50px;"></iframe>');
	}
}

var uniqueId;
function loadAd()
{
	var rr = Math.floor(Math.random()*756843);
	var dd = new Date().getTime();
	uniqueId = dd + "-" + rr;
	AppMobi.advertising.getAd( appMobiAdName, appMobiAdPath, uniqueId );
}

function getAppMobiObjects()
{
	if( typeof parent.AppMobi == "undefined" || parent.AppMobi.available == false )
	{
		setTimeout( "getAppMobiObjects();", 50 );
	}
	else
	{
		AppMobi = parent.AppMobi;
		amDocument = parent.document;
		amDocument.addEventListener( "appMobi.advertising.ad.load", onAdLoaded, false);
		loadAd();
	}
}

function getAppMobiObject()
{
	if( typeof AppMobi == "undefined" )
		return null;

	return AppMobi;
}

function getAppMobiDocument()
{
	return amDocument;
}

getAppMobiObjects();
