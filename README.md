# DroneMR


<img src="https://github.com/PALequeu/DroneMR/assets/96840467/203f6b3f-325e-486f-bb99-b1db42dc7f88" width="300" height="300">


## Introduction & Actual State of the Project

DroneMR is a mobile app developped by Pierre-Antoine Lequeu as a school project.  
The app is developped in kotlin using Android Studio. 

Its aim is to follow and control Parrot Anafi and Anafi AI drone (and potentially all drone using parrot's GroundSDK & AirSDK APIs) using either a parrot controler or mavlink missions generation.

At this point in time, it can connect to a drone and a parrot remote controller, monitor their state (battery left, position, number of sattelite(s) connected) and give basic control order to a drone : Take Off & Land.  
Mavlink mission generation and sending to a drone are implemented, but is yet to be tested, as the drone can not be used in public places.  
The application is also adapted to work for the CoHoMa missions of the french army. It can send positional data to a server for this purpose. This will be useful in the future to use the drone position and send it to other drones in orther to make flocks of drone fly together, which is the final objective of the app.

In the folder is included a DronerMR_AmbientSystem.pdf file that is the slides of the oral presentation of the project for my Ambient System course with Paul Chriqui.

## User Interface 

The interface gives important information about the drone and its controller, such as if it is connected to the app, the remaining battery of both of them, and the number of sattelites the drone is connected to (which gives information about the quality of the GPS).  
<p align="center">
<img src="https://github.com/PALequeu/DroneMR/assets/96840467/f8dd7798-2969-4cc6-8a88-0c02a6c59c9f" height="500" width="auto" />
</p>
The two blue bottom right buttons allow you to zoom in on the drone when it is connected for the top one, and follow the drone when it is moving for the bottom one. On the bottom part the application are five buttons :   
  - Config allows you to change informations of the mission you are generating, of the CoHoMa configuration, and change the server you are sending data to.  
  - Generate generates the mavlink mission file related to all the waypoint you added on the map, in the right order.  
  - Start (active when Generate has been pressed) sends the mission to the drone and starts it.  
  - Stop (active when Generate has been pressed as well) stop the ongoing mission.  
<p align="center">
<img src="https://github.com/PALequeu/DroneMR/assets/96840467/4ce05ed4-375d-4cae-b1fa-d34d311e91e3" height="500" width="auto" />
</p>
The last button is the "Take Off" button that, as one could imagine, makes the drone take off. It then changes into a "Land" button to make the drone land.  

A drawer has also been implemented. At the moment, it has no use, but its aim is to be able to have a window to display the drone camera, and to access the videos saved on the drone.

<p align="center">
<img src="https://github.com/PALequeu/DroneMR/assets/96840467/1b83c611-8dbe-4692-85ba-2c76e024bdef" height="500" width="auto" />
</p>

## Technology used

<p align="center">
<img src="https://github.com/PALequeu/DroneMR/assets/96840467/5fefe2e4-5ccd-412e-8035-ec71ec9c3f44" height="500" width="auto" />
</p>
As mentionned before, the application is developped in Kotlin using Android Studio. Three technologies makes the entire application :  
  - GroundSDK is the API developped by Parrot in order to develop web apps and mobile apps in order to interact with parrot drones. You can find the documentation [here](https://developer.parrot.com/docs/refdoc-android/index.html).  
  - Google Maps API is the API developped by google to access their map service and develop app around it. It is quite intuitive and very permissive. You can find their website [here](https://developers.google.com/maps?hl=fr).  
  - OkHTTP3 is a free open source API to make HTTP request that handle common connection problems by itself. [Here](https://square.github.io/okhttp/) is their website.

## Perspectives

The next step in the project is the implement the camera monitoring, which is an important feature of the app. The "makes flocks of drone fly together" part is handled by another student of the Mines Nancy school of engineering, but I will probably have to also work on it in order for it to work well with the app.  
Then, the idea is the use the flocks of drone to monitor areas to prevent poaching, to look for structural problems on nuclear reactor, or to access and analyze unreacheable areas.  
All that could be made through deep learning, either directly on the drone, or on another computer.





