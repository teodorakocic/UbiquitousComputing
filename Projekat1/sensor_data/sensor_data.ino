#include <Arduino_LPS22HB.h>  //Air pressure and temperature sensor
#include <Arduino_APDS9960.h> //Color, gesture and proximity sensor
#include <Arduino_LSM9DS1.h> //Accelerometer

 
void setup() {
  Serial.begin(9600);
  while (!Serial);

  if (!BARO.begin()) {
    Serial.println("Failed to start the LPS22HB sensor!");
    while (1);
  }

  if (!APDS.begin()) {
    Serial.println("Failed to start APDS9960 sensor!");
    while (1);
  }

  if (!IMU.begin()) {
    Serial.println("Failed to start LSM9DS1 sensor!");
    while (1);
  }
}

void loop() {
  float temp = BARO.readTemperature();  //in C
  float pressure = BARO.readPressure(); //in kPa
  
  int proximity = APDS.readProximity();

  int r, g, b, a;
  APDS.readColor(r, g, b, a);

  float x, y, z;
  IMU.readAcceleration(x, y, z);

  while (!APDS.colorAvailable() || !APDS.proximityAvailable()) {
  }

  Serial.print(temp);
  Serial.print(',');
  Serial.print(pressure);
  Serial.print(',');
  Serial.print(proximity);
  Serial.print(',');
  Serial.print(r);
  Serial.print(',');
  Serial.print(b);
  Serial.print(',');
  Serial.print(g);
  Serial.print(',');
  Serial.print(a);
  Serial.print(',');
  Serial.print(x);
  Serial.print(',');
  Serial.print(y);
  Serial.print(',');
  Serial.print(z);
  
  Serial.println();

  delay(2500);
}
