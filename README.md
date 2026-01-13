# Welcome to Buoy Finder!
## About:
Buoy Finder is an Android app that allows users to locate SPOT TRACE devices. Users can view the coordinates of their deployed SPOT devices, their distance and heading relative to the devices.

Buoy Finder is useful for a few reasons. First, SPOT's mobile app doesn't use the GPS coordinates of a user's phone, so it can't calculate distance or heading relative to the device. Secondly, Buoy Finder 
is lightweight and can run well on older devices. Lastly, Buoy Finder allows device data to be kept for offline use. For example, when before a user leaves an area will cell coverage they can pull device data and
still see their own location using their phones GPS receiver.

## Getting Started:

1. Download the app-debug.apk from the releases section on the right. 

2. Open the file on your Android device.

3. If prompted, allow "Install from Unknown Sources" in your browser or file manager settings.

4. If a "Play Protect" warning appears, click "Install Without Scanning" or "Scan with Google"

Here's what the app should look like:

<p align="center">
  <img src="https://github.com/user-attachments/assets/d91ec712-0f5b-4099-8b1a-164e8259d935" width="300" height="650" alt="Buoy Finder Screenshot">
  <img src="https://github.com/user-attachments/assets/57a5cf62-d89b-4c66-b3ce-c6f8539dd792" width="300" height="650" alt="Buoy Finder Screenshot">
</p>

*Note: My low-budget Samsung A01 doesn't have a magnetometer or accelerometer. Without these, "Currently Facing" won't show.*

The top left button is for selecting which asset's information you want to view. Selecting it will show you:
 - The assets GPS coordinates
 - Date and time of most recent update
 - Your distance from asset
 - Bearing to asset

Additionally, the direction you're currently moving and pointed towards is shown below the above info. Some devices without an magnetometer won't be able to show direction you're facing.

The map shows the coast of Ghana with bathymetry lines. Assets are projected on the map. Their color (green, yellow, or red) is based on how recently they sent updates.

If you find any bugs in the app, please create a new issue describing it.

## Who:

This project is being built and maintained by Ethan Brooks. The project is under the guidance of Omand Lab at University of Rhode Island Graduate School of Oceanography, whom the app is being developed for. Still, this app is open for anyone's use who needs to track a their SPOT TRACE devices.
