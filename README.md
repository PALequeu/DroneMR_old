# DroneMR

DroneMR is a mobile app developped by Pierre-Antoine Lequeu as a school project.
The app is developped in kotlin using Android Studio. 

Its aim is to follow and control Parrot Anafi and Anafi AI drone (and potentially all drone using parrot's GroundSDK & AirSDK APIs) using either a parrot controler or mavlink missions generation.

At this point in time, it can connect to a drone and a controler, monitor their state (battery left, position, number of sattelite(s) connected) and give basic control order to a drone : Take Off & Land. 
Mavlink mission generation and sending to a drone are implemented, but is yet to be tested, as the drone can not be used in public places.

The application is also adapted to work for the CoHoMa missions for the french army. It can send positional data to a server for this purpose. This will be useful in the future to use the drone position and send it to other drones in orther to make flocks of drone fly together, which is the final objective of the app.

In the folder is included a DronerMR_AmbientSystem.pdf file that is the slides of the oral presentation of the project for my Ambient System course with Paul Chriqui.
