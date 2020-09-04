# SimViewer
![Example: EmbryoGen (C++) sends images to scenerygraphics/scenery (Java + Kotlin)](https://www.fi.muni.cz/~xulman/files/TRAgen-demo/EmbryoGen_SimViewer.png)
![Example text: EmbryoGen (C++) sends images to ImgViewer (Java) in Fiji](https://www.fi.muni.cz/~xulman/files/TRAgen-demo/EmbryoGen_ImgViewer.png)

# About
This is essentially a [Scenery](https://github.com/scenerygraphics/scenery) 'client' that [ZeroMQ](https://github.com/zeromq/jeromq)-ish listens for and displays current geometry of the simulated agents. The communication is based on own/proprietary (and simple) network protocol.

Recently, and ImgViewer has been added which is essentially a [Fiji](http://fiji.sc) plugin that [DAIS-wp1.3](https://github.com/xulman/DAIS-wp1.3)-ish listens and displays current raw/voxel images of the simulated agents. The communication is based again on own/proprietary (and also simple, but different from the one above) network protocol.

One can find the simulator that feeds this display in [another repo](https://github.com/xulman/EmbryoGen).

# Installing and starting this client
```
git clone https://github.com/xulman/SimViewer.git
cd SimViewer/
mvn dependency:copy-dependencies package
cd target
java -cp "SimViewer-1.0-SNAPSHOT.jar:dependency/*" de.mpicbg.ulman.simviewer.StartUpScene
```

*Note*: The DAIS image transfer is not published with maven. One may need to [download](https://raw.githubusercontent.com/xulman/DAIS-wp1.3/master/release/DAIS-wp13-1.0.1.jar) and install it on ones own.
*Note*: The SimViewer in a form of [SciView](https://github.com/scenerygraphics/sciview/) plugin is underway and shall be released soon.
