#include <Arduino_LPS22HB.h> // Air pressure sensor
#include <Arduino_APDS9960.h> // Color, gesture, and proximity sensor
#include <Arduino_LSM9DS1.h> // Accelerometer
#include <ArduinoBLE.h>
//#include <TensorFlowLite.h>
//#include "main_functions.h"
//#include "detection_responder.h"
//#include "image_provider.h"
//#include "model_settings.h"
//#include "person_detect_model_data.h"
//#include "tensorflow/lite/micro/micro_error_reporter.h"
//#include "tensorflow/lite/micro/micro_interpreter.h"
//#include "tensorflow/lite/micro/micro_mutable_op_resolver.h"
//#include "tensorflow/lite/schema/schema_generated.h"
//#include "tensorflow/lite/version.h"

const int ledPin = LED_BUILTIN;
const int buttonPin = 4;

BLEService ledService("19B10010-E8F2-537E-4F6C-D104768A1214");
BLEByteCharacteristic ledCharacteristic("19B10011-E8F2-537E-4F6C-D104768A1214", BLERead | BLEWrite);
BLEByteCharacteristic buttonCharacteristic("19B10012-E8F2-537E-4F6C-D104768A1214", BLERead | BLENotify);

//namespace {
//tflite::ErrorReporter* error_reporter = nullptr;
//const tflite::Model* model = nullptr;
//tflite::MicroInterpreter* interpreter = nullptr;
//TfLiteTensor* input = nullptr;
//constexpr int kTensorArenaSize = 136 * 1024;
//static uint8_t tensor_arena[kTensorArenaSize];
//}

void blinkLED(int pin)
{
  for (int i = 0; i < 5; i++)
  {
    digitalWrite(pin, HIGH);
    delay(500);
    digitalWrite(pin, LOW);
    delay(500);
  }
}

void offLED()
{
  digitalWrite(LEDG, HIGH);
  digitalWrite(LEDR, HIGH);
  digitalWrite(LEDB, HIGH);
}

void setup() {
  Serial.begin(9600);
  while (!Serial);

  pinMode(ledPin, OUTPUT);
  pinMode(buttonPin, INPUT); 
  
  if (!BLE.begin()) {
    while (1);
  }

  BLE.setLocalName("NANO 33 BLE");
  BLE.setAdvertisedService(ledService);
  
  ledService.addCharacteristic(ledCharacteristic);
  ledService.addCharacteristic(buttonCharacteristic);

  BLE.addService(ledService);

  ledCharacteristic.writeValue(0);
  buttonCharacteristic.writeValue(0);

  BLE.advertise();
//  Serial.println("Bluetooth® device active, waiting for connections...");/

  if (!BARO.begin()) {
    Serial.println("Failed to start the LPS22HB sensor.");
    while (1);
  }

  if (!APDS.begin()) {
    Serial.println("Failed to start the APDS9960 sensor.");
    while (1);
  }

  if (!IMU.begin()) {
    Serial.println("Failed to start the LSM9DS sensor.");
    while (1);
  }

//  //ML
//
//  static tflite::MicroErrorReporter micro_error_reporter;
//  error_reporter = &micro_error_reporter;
//  model = tflite::GetModel(g_person_detect_model_data);
//  if (model->version() != TFLITE_SCHEMA_VERSION) {
//    TF_LITE_REPORT_ERROR(error_reporter,
//                         "Model provided is schema version %d not equal "
//                         "to supported version %d.",
//                         model->version(), TFLITE_SCHEMA_VERSION);
//    return;
//  }
//  static tflite::MicroMutableOpResolver<5> micro_op_resolver;
//  micro_op_resolver.AddAveragePool2D();
//  micro_op_resolver.AddConv2D();
//  micro_op_resolver.AddDepthwiseConv2D();
//  micro_op_resolver.AddReshape();
//  micro_op_resolver.AddSoftmax();
//  static tflite::MicroInterpreter static_interpreter(
//      model, micro_op_resolver, tensor_arena, kTensorArenaSize, error_reporter);
//  interpreter = &static_interpreter;
//  TfLiteStatus allocate_status = interpreter->AllocateTensors();
//  if (allocate_status != kTfLiteOk) {
//    TF_LITE_REPORT_ERROR(error_reporter, "AllocateTensors() failed");
//    return;
//  }
//  input = interpreter->input(0);
}

void loop() {
  BLE.poll();

  char buttonValue = digitalRead(buttonPin);
  bool buttonChanged = (buttonCharacteristic.value() != buttonValue);

  if (buttonChanged) {
    ledCharacteristic.writeValue(buttonValue);
    buttonCharacteristic.writeValue(buttonValue);
  }

  if (ledCharacteristic.written() || buttonChanged)
  {
    if (ledCharacteristic.value()) 
    {
      if (ledCharacteristic.value() == 2) 
      {
         blinkLED(ledPin);
      } else if (ledCharacteristic.value() == 3) 
      {
         digitalWrite(LEDR, LOW);
         digitalWrite(LEDG, HIGH);
         digitalWrite(LEDB, HIGH);
         delay(500);
         blinkLED(LEDR);
         offLED();
      } else if (ledCharacteristic.value() == 4)
      {
        digitalWrite(LEDG, LOW);
        digitalWrite(LEDR, HIGH);
        digitalWrite(LEDB, HIGH);
        delay(500);
        blinkLED(LEDG);
        offLED();
      } else if (ledCharacteristic.value() == 5)
      {
        digitalWrite(LEDB, LOW);
        digitalWrite(LEDR, HIGH);
        digitalWrite(LEDG, HIGH);
        delay(500);
        blinkLED(LEDB);
        offLED();
      }
      
//      Serial.println("LED on");
      digitalWrite(ledPin, HIGH);
    } else {
//      Serial.println("LED off");
      digitalWrite(ledPin, LOW);
    }
  }
   
  float pressure = BARO.readPressure(); // In kPa
  float temp = BARO.readTemperature();
    
  int gesture = APDS.readGesture();
  int proximity = APDS.readProximity();
  
  int r, g, b, a;
  APDS.readColor(r, g, b, a);

  float accX, accY, accZ;
  IMU.readAcceleration(accX, accY, accZ);

  float magX, magY, magZ;
  IMU.readMagneticField(magX, magY, magZ);

  float gyrX, gyrY, gyrZ;
  IMU.readGyroscope(gyrX, gyrY, gyrZ);

  while (!APDS.colorAvailable() || !APDS.proximityAvailable())
  {
  }

//  if (kTfLiteOk != GetImage(error_reporter, kNumCols, kNumRows, kNumChannels,
//                            input->data.int8)) {
//    TF_LITE_REPORT_ERROR(error_reporter, "Image capture failed.");
//  }
//  if (kTfLiteOk != interpreter->Invoke()) {
//    TF_LITE_REPORT_ERROR(error_reporter, "Invoke failed.");
//  }
//
//  TfLiteTensor* output = interpreter->output(0);
//  int8_t person_score = output->data.uint8[kPersonIndex];
//  int8_t no_person_score = output->data.uint8[kNotAPersonIndex];
//  RespondToDetection(error_reporter, person_score, no_person_score);

  Serial.print(pressure);
  Serial.print(',');
  Serial.print(temp); 
  Serial.print(',');
  Serial.print(proximity);
  Serial.print(',');
  Serial.print(gesture);
  Serial.print(',');
  Serial.print(r);
  Serial.print(',');
  Serial.print(g);
  Serial.print(',');
  Serial.print(b);
  Serial.print(',');
  Serial.print(a);
  Serial.print(',');
  Serial.print(accX);
  Serial.print(',');
  Serial.print(accY);
  Serial.print(',');
  Serial.print(accZ);
  Serial.print(',');
  Serial.print(magX);
  Serial.print(',');
  Serial.print(magY);
  Serial.print(',');
  Serial.print(magZ);
  Serial.print(',');
  Serial.print(gyrX);
  Serial.print(',');
  Serial.print(gyrY);
  Serial.print(',');
  Serial.print(gyrZ);
  Serial.println();
}
