package fm.flycast;

public class XMLFail extends XMLObject
{
    public String message = null;

    XMLFail()
    {
        type = XMLObject.FAIL;
        children = null;
    }
}