package de.mpicbg.ulman.simviewer.util;


import cleargl.GLVector;
import graphics.scenery.Material;

/**
 * Container of differently colored materials, materials are/look otherwise the same,
 * with utility methods to find material of a color that is closest to the asked color.
 * User is asked to setup the material properties with setMaterialsAlike() after
 * the object of this class is created.
 *
 * RGB values must be in the interval [0,1].
 * rgb[0] is red, rgb[1] is green, rgb[2] is blue.
 */
public class Palette
{
	/** default palette is 5*5*5 = 125 different RGB triplets */
	public Palette()
	{ setupMaterials(0.2f); }

	/** palette with given amount of recognized "shades" within one color
	    channel, note that this will create binsPerChannel^3 Material objects */
	public Palette(final int binsPerChannel)
	{ setupMaterials(1.f / (float)(binsPerChannel)); }

	/** palette with given distance between recognized "shades" within one color
	    channel, note that this will create (1/binSizePerChannel)^3 Material objects */
	public Palette(final float binSizePerChannel)
	{ setupMaterials(binSizePerChannel); }


	/** returns a material of the color closest to the desired one */
	public Material getMaterial(final float r,final float g,final float b)
	{
		return materials[ rgbToIndex(r,g,b) ];
	}

	/** returns a material of the color closest to the desired one */
	public Material getMaterial(final GLVector rgbOrRgba)
	{
		return materials[ rgbToIndex(rgbOrRgba.x(),rgbOrRgba.y(),rgbOrRgba.z()) ];
	}

	/** returns a material of the color closest to the desired color from
	    the original SimViewer's color palette (according to the "v1" protocol) */
	public Material getMaterial(final int colorIndex)
	{
		int mIdx;
		switch (colorIndex)
		{
		case 1:
			mIdx = rgbToIndex(1.0f, 0.0f, 0.0f);
			break;
		case 2:
			mIdx = rgbToIndex(0.0f, 1.0f, 0.0f);
			break;
		case 3:
			mIdx = rgbToIndex(0.2f, 0.4f, 1.0f); //lighter blue
			break;
		case 4:
			mIdx = rgbToIndex(0.0f, 1.0f, 1.0f);
			break;
		case 5:
			mIdx = rgbToIndex(1.0f, 0.0f, 1.0f);
			break;
		case 6:
			mIdx = rgbToIndex(1.0f, 1.0f, 0.0f);
			break;
		default:
			mIdx = rgbToIndex(1.0f,1.0f,1.0f);
		}
		return materials[ mIdx ];
	}

	/** resets all materials to look similar to the template material
	    except for the diffusive colors */
	public void setMaterialsAlike(final Material template)
	{
		for (Material material : materials)
		{
			//FORBIDDEN// material.setDiffuse();
			material.setAmbient( template.getAmbient() );
			material.setSpecular( template.getSpecular() );
			material.setCullingMode( template.getCullingMode() );
		}
	}

	/** shortcut method to enable culling of front faces in
	    all underlying materials */
	public void EnableFrontFaceCulling()
	{
		for (Material m : materials)
			m.setCullingMode(Material.CullingMode.Front);
	}

	/** shortcut method to disable culling of front faces in
	    all underlying materials */
	public void DisableFrontFaceCulling()
	{
		for (Material m : materials)
			m.setCullingMode(Material.CullingMode.None);
	}


	/** number of bins per color channel: bins*binWidth >= 1 */
	private int bins;
	/** width of one bin per color channel: bins*binWidth >= 1 */
	private float binWidth;

	/** materials to choose from, they differ only in their diffusive colors */
	private Material[] materials;


	/** reports rgb values of a material at index i,
	    rgb[0] is red, rgb[1] is green, rgb[2] is blue. */
	protected void indexToRgb(final int i, final float[] rgb)
	{
		final int r = i / (bins*bins);
		final int g = (i - r*bins*bins) / bins;
		final int b = i % bins;

		rgb[0] = r*binWidth;
		rgb[1] = g*binWidth;
		rgb[2] = b*binWidth;
	}

	/** returns index to materials array that is closest to the given color,
	    rgb[0] is red, rgb[1] is green, rgb[2] is blue. */
	protected int rgbToIndex(final float[] rgb)
	{ return rgbToIndex(rgb[0],rgb[1],rgb[2]); }

	/** returns index to materials array that is closest to the given color */
	protected int rgbToIndex(final float r,final float g,final float b)
	{
		//make sure bin index is within its upper bound
		//(since 1.0f qualifies for an extra bin when bins*binWidth = 1.0)
		final int rr = Math.min( (int)(r / binWidth), bins-1 );
		final int gg = Math.min( (int)(g / binWidth), bins-1 );
		final int bb = Math.min( (int)(b / binWidth), bins-1 );
		return ( rr*bins*bins + gg*bins + bb );
	}


	private void setupMaterials(final float binSizePerChannel)
	{
		if (binSizePerChannel <= 0.1f)
			throw new RuntimeException("Negative or just way too small bin size (" + binSizePerChannel + ")");

		bins = (int)Math.ceil(1.0f / binSizePerChannel);
		binWidth = binSizePerChannel;
		materials = new Material[ bins*bins*bins ];

		float[] rgb = new float[3];
		for (int i=0; i < materials.length; ++i)
		{
			indexToRgb(i,rgb);
			//DEBUG// System.out.println(i+": "+rgb[0]+","+rgb[1]+","+rgb[2]);

			materials[i] = new Material();
			materials[i].setDiffuse( new GLVector(rgb[0],rgb[1],rgb[2]) );
		}
	}
}
