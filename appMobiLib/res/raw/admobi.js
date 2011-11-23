var amDocument = null;
var AppMobi = {};

try
{
	AppMobi = parent.getAppMobiObject();
}
catch(e)
{
	if( typeof console != "undefined" && typeof console.log != "undefined" )
		console.log( "appMobi XDK -- error getting parent AppMobi" );
};

try
{
	amDocument = parent.getAppMobiDocument();
}
catch(e)
{
	if( typeof console != "undefined" && typeof console.log != "undefined" )
		console.log( "appMobi XDK -- error getting parent document" );
};
