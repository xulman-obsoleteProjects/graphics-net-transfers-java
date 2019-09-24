package de.mpicbg.ulman.simviewer;


import cleargl.GLVector;
import de.mpicbg.ulman.simviewer.util.Palette;

public class PaletteTest
{
	public static void main(String... args)
	{
		Palette p = new Palette(3);

		System.out.println("binCount = 3");
		reportMatching(p, 0.2f,0.2f,0.2f);
		reportMatching(p, 0.2f,0.4f,0.2f);
		reportMatching(p, 0.4f,0.2f,0.4f);
		reportMatching(p, 0.9f,0.9f,0.9f);
		reportMatching(p, 1.0f,1.0f,1.0f);

		System.out.println("binWidth = 0.8");
		p = new Palette(0.8f);
		reportMatching(p, 0.7f,0.9f,0.4f);
		reportMatching(p, 0.9f,0.9f,0.9f);
		reportMatching(p, 1.0f,1.0f,1.0f);

		System.out.println("binCount = 5");
		p = new Palette();
		reportMatching(p, 0);
		reportMatching(p, 1);
		reportMatching(p, 2);
		reportMatching(p, 3);
		reportMatching(p, 4);
		reportMatching(p, 5);
		reportMatching(p, 6);

		reportMatching(p, 0.99f,0.0f,0.0f);
		reportMatching(p, 0.0f,0.99f,0.0f);
		reportMatching(p, 0.0f,0.0f,0.99f);
		reportMatching(p, 1.0f,0.0f,0.0f);
		reportMatching(p, 0.0f,1.0f,0.0f);
		reportMatching(p, 0.0f,0.0f,1.0f);
	}

	private static void reportMatching(final Palette p, final float r,final float g,final float b)
	{
		final GLVector color = p.getMaterial(r,g,b).getDiffuse();
		System.out.println(r + "," + g + "," + b + "  ->  " + color.x() + "," + color.y() + "," + color.z());
	}

	private static void reportMatching(final Palette p, final int index)
	{
		final GLVector color = p.getMaterial(index).getDiffuse();
		System.out.println("index "+index+"  ->  " + color.x() + "," + color.y() + "," + color.z());
	}
}
