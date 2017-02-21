# GarageDoorPI


This project was gleaned from a few different sources. I will try to give credit where I can, and I sincerely apologize if I missed anyone. I will admit that most of this code is not mine, I just kluged it together.

First off, I followed most of the instructions from: http://www.sampaulling.com/raspberry-pi-garage-door-opener/. I will go through some of the steps below to summarize because I didn’t follow everything to the letter.

Used https://github.com/chancsc/SC_SmartThings/blob/master/IR-Remote/device-handler/HTTP_Button.groovy to modify the Z-Wave Door/Window sensor to add a button with a call to my raspberry pi webpage.

I purchased the following: Raspberry PI 3 model B, SunFounder 5V 4 Channel Relay (I have a 3 car garage, so if you only have 2, you can get a 2 channel), Phantom YoYo 40P Dupont Cable 10cm Female to Female 1P to 1P, Raspberry Pi 3 kit with clear case and 16GB SD Card. I also purchased an Ecolink Intelligent Technology Z-Wave Garage Door Tilt Sensor.

Installed NOOBS: https://www.raspberrypi.org/downloads/noobs/ on the SD Card, then installed Raspbian full. 
Updated and downloaded necessary components:
  sudo apt-get install git-core
  sudo apt-get update 
  sudo apt-get upgrade
  git clone git://git.drogon.net/wiringPi
  cd wiringPi 
  git pull origin
  cd wiringPi 
  ./build
	sudo apt-get update
  sudo apt-get install apache2 php5 libapache2-mod-php5

Put the index.php into /var/www/html

Wire up the Pi to the Relay as shown on http://www.sampaulling.com/raspberry-pi-garage-door-opener/. If you have more than one garage door, wire up the extra pins as well.

Modify index.php to activate the pins needed. I will say that this took a little playing around, and I will admit that I’m a total Noob when it comes to Raspberry PI. I played with the gpio mode # out/in command until I activated the pins I needed.

URL to actuate relay: http://ip.add.re.ss/?trigger={1,2,3} (1 for relay 1, 2 for relay 2, 3 for relay 3. For example if I want to trigger garage door 3 my URL will be http://garagepi/?trigger=3.

I ended up adding my Z-Wave Tilt Sensors first, then created a custom device handler using the Z-Wave door-window sensor with HttpButton.groovy code I modified, then updated the device to use the custom device handler. This allows you to go into your tilt sensor on Smartthings to see door status and a button to actuate the relay. The first time you go to your device in Smartthings, click the gear icon to set up the http button.

