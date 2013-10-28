StudioManager-IOServer
======================

An attempt to bring a recording studio automation to Java using Ethernet based actors and sensors.

It's kind of a home automation but with recording studio focus. The following protocols / products are implemented:

- Snom Vision. The server simulates a snom phone so the vision can be used in stand alone mode to act as a launch pad. This is done by reverse engineering to protocoll and using some of the documentation of csta.
- Modtronix SBC65EC as a bridge from Ethernet (UDP) to:
  - RS232 (generic and transparent)
  - LCD screen (4x20 as delieverd from Modtronix) - actually on the I2C bus
  - I2C bus (generic and transparent but also with some actualy sensors such as temp and humidity)
  - GPIO
  - PWM outs
  - Analog ins
  - Analog outs
- DAENet SNMP based relais board
- HD44780 over GPIO or I2C GPIO based GPIO driver MCP23008 which is implemented as well
- DMX Agent for dmx4all dmx interfaces (but not Artnet)
- Midi to interface with your DAW. This is used to know if the producer is recording (and how many tracks) to do something meaningful (such as letting visitors wait in front of the door by writing a message to the LCD)
- REST interface (currently not fully implemented)

the framework itself uses PluginService to load the services that the user needs. A plugin can be an agent if it implements one of the above mentioned protocols.
all rules and values are stored in a database by using hibernate.
There are triggers (kind of sensors) and actions (kind of actors). Based on a quasi-language binding is done with the agents.


At least this was the intention and it worked perfectly for me - however I didn't finalize the project due to time shortage and switched to openhab.org
Because of this I pusblished the code to enable development of some openhab bindings (in particular the Snom Vision).

Please treat this more as an information share rather a fully working product. Feel free to use whatever you can use but respect GPL if there are some.

leutholl@gmail.com
