#ifndef SHAPEHINTER_H
#define SHAPEHINTER_H

#include "../util/report.h"
#include "AbstractAgent.h"
#include "../Geometries/ScalarImg.h"

class ShapeHinter: public AbstractAgent
{
public:
	/** the same (given) shape is kept during the simulation */
	ShapeHinter(const int ID, const std::string& type,
	            const ScalarImg& shape,
	            const float currTime, const float incrTime)
		: AbstractAgent(ID,type, geometryAlias, currTime,incrTime),
		  geometryAlias(shape)
	{
		//update AABBs
		geometryAlias.Geometry::updateOwnAABB();

		DEBUG_REPORT("EmbryoShell with ID=" << ID << " was just created");
		DEBUG_REPORT("AABB: " << geometryAlias.AABB.minCorner << " -> " << geometryAlias.AABB.maxCorner);
	}

	~ShapeHinter(void)
	{
		DEBUG_REPORT("EmbryoShell with ID=" << ID << " was just deleted");
	}


private:
	// ------------- internals state -------------

	// ------------- internals geometry -------------
	/** reference to my exposed geometry ShadowAgents::geometry */
	ScalarImg geometryAlias;

	/** my internal representation of my geometry, which is exactly
	    of the same form as my ShadowAgent::geometry */
	//ScalarImg futureGeometry;

	// ------------- externals geometry -------------

	// ------------- to implement one round of simulation -------------
	void advanceAndBuildIntForces(const float) override
	{

		//increase the local time of the agent
		currTime += incrTime;
	}

	void adjustGeometryByIntForces(void) override
	{
		//would update my futureGeometry
	}

	void collectExtForces(void) override
	{
		//hinter is not responding to its surrounding
	}

	void adjustGeometryByExtForces(void) override
	{
		//would update my futureGeometry
	}

	//futureGeometry -> geometryAlias
	void updateGeometry(void) override
	{
		//since we're not changing ShadowAgent::geometry (and consequently
		//not this.geometryAlias), we don't need to update this.futureGeometry
	}

	// ------------- rendering -------------
	void drawForDebug(DisplayUnit& du) override
	{
		if (detailedDrawingMode)
		{
			int dID = ID << 17 | 1 << 16; //enable debug bit

			//draw bounding box of the complete ScalarImg
			dID += geometryAlias.AABB.drawBox(dID,4,
			  geometryAlias.getDistImgOff(),geometryAlias.getDistImgFarEnd(), du);

			//TODO: render spheres along a certain isoline,
			//      with user given sparsity
			REPORT(IDSIGN << "not implemented yet...");
		}
	}

	void drawForDebug(i3d::Image3d<i3d::GRAY16>& img) override
	{
		//shortcuts to the mask image parameters
		const i3d::Vector3d<float>& res = img.GetResolution().GetRes();
		const Vector3d<FLOAT>       off(img.GetOffset().x,img.GetOffset().y,img.GetOffset().z);

		//shortcuts to our own geometry
		const i3d::Image3d<float>&   distImg = geometryAlias.getDistImg();
		const Vector3d<FLOAT>&    distImgRes = geometryAlias.getDistImgRes();
		const Vector3d<FLOAT>&    distImgOff = geometryAlias.getDistImgOff();
		const ScalarImg::DistanceModel model = geometryAlias.getDistImgModel();

		//project and "clip" this AABB into the img frame
		//so that voxels to sweep can be narrowed down...
		//
		//   sweeping position and boundaries (relevant to the 'img')
		Vector3d<size_t> curPos, minSweepPX,maxSweepPX;
		geometryAlias.AABB.exportInPixelCoords(img, minSweepPX,maxSweepPX);
		//
		//micron coordinate of the running voxel 'curPos'
		Vector3d<FLOAT> centre;
		//
		//px coordinate of the voxel that is counterpart in distImg to the running voxel
		Vector3d<size_t> centrePX;

		//sweep within the intersection of the 'img' and geometryAlias::distImg
		for (curPos.z = minSweepPX.z; curPos.z < maxSweepPX.z; curPos.z++)
		for (curPos.y = minSweepPX.y; curPos.y < maxSweepPX.y; curPos.y++)
		for (curPos.x = minSweepPX.x; curPos.x < maxSweepPX.x; curPos.x++)
		{
			//get micron coordinate of the current voxel's centre
			//NB: AABB.exportInPixelCoords() assures that voxel centres fall into AABB
			centre.x = ((FLOAT)curPos.x +0.5f) / res.x;
			centre.y = ((FLOAT)curPos.y +0.5f) / res.y;
			centre.z = ((FLOAT)curPos.z +0.5f) / res.z;
			centre += off;

			//project the voxel's 'centre' to the geometryAlias.distImg
			centre -= distImgOff;
			centre.elemMult(distImgRes); //in px & in real coords

#ifdef DEBUG
			if (centre.x < 0 || centre.y < 0 || centre.z < 0
			 || centre.x >= distImg.GetSizeX() || centre.y >= distImg.GetSizeY() || centre.z >= distImg.GetSizeZ())
				REPORT(ID << " gives counter-voxel " << centre << " outside of the distImg");
#endif

			//down-round to find the voxel that holds this (real)coordinate
			centrePX.x = (size_t)centre.x;
			centrePX.y = (size_t)centre.y;
			centrePX.z = (size_t)centre.z;
			//extract the value from the distImg
			const float dist = distImg.GetVoxel(centrePX.x,centrePX.y,centrePX.z);

			if (dist < 0 || (dist == 0 && model == ScalarImg::DistanceModel::ZeroIN_GradOUT))
			{
#ifdef DEBUG
				i3d::GRAY16 val = img.GetVoxel(curPos.x,curPos.y,curPos.z);
				if (val > 0 && val != (i3d::GRAY16)ID)
					REPORT(ID << " overwrites mask at " << curPos);
#endif
				img.SetVoxel(curPos.x,curPos.y,curPos.z, (i3d::GRAY16)ID);
				//NB: should dilate by 1px for model == GradIN_ZeroOUT
			}
		}
	}
};
#endif
