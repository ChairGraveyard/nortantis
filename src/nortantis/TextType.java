package nortantis;

public enum TextType
{
	Title,
	Region,
	Mountain_range,
	Other_mountains,
	River;
	
	public String toString() 
	{
        return name().replace("_"," ");
    }
}