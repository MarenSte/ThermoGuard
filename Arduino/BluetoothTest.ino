#include "DHT.h"
#include <SoftwareSerial.h>
 
#define DHTPIN 2     // what digital pin we're connected to
#define DHTTYPE DHT22   // DHT 22  (AM2302), AM2321

DHT dht(DHTPIN, DHTTYPE);
SoftwareSerial bluetooth(0, 1); // TX, RX
char blueToothVal; //Werte sollen per Bluetooth gesendet werden
char lastValue;   //speichert den letzten Status der LED (on/off) 
char data;
 float h;
 float t;


void setup(){
 //Serial.begin(9600);  //serieller Monitor wird gestartet, Baudrate auf 9600 festgelegt
 dht.begin();
 bluetooth.begin(9600);
 bluetooth.println("Bluetooth On");
}
 
void loop(){
  
  h = dht.readHumidity();
  // Read temperature as Celsius (the default)
  t = dht.readTemperature();
//if (bluetooth.available()){
        
        data=bluetooth.read();
       // Serial.println(data);
        bluetooth.print(t);
        bluetooth.print(",");
        bluetooth.print(h);
        bluetooth.println(";");
  // }  

  
  
  // Printing the results on the serial monitor
//  Serial.print("Temperature = ");
//  Serial.print(t);
//  Serial.print(" *C ");
//  Serial.print("    Humidity = ");
//  Serial.print(h);
//  Serial.println(" % ");
  
  delay(60000); // Delays 2 secods, as the DHT22 sampling rate is 0.5Hz
 }
