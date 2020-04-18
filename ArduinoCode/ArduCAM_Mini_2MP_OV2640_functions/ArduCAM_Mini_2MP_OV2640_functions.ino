#include <Wire.h>
#include <ArduCAM.h>
#include <SPI.h>
#include "memorysaver.h"
#include <SoftwareSerial.h>

SoftwareSerial mySerial(2, 3); // RX, TX

#define BMPIMAGEOFFSET 66
const char bmp_header[BMPIMAGEOFFSET] PROGMEM =
{
  0x42, 0x4D, 0x36, 0x58, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x42, 0x00, 0x00, 0x00, 0x28, 0x00,
  0x00, 0x00, 0x40, 0x01, 0x00, 0x00, 0xF0, 0x00, 0x00, 0x00, 0x01, 0x00, 0x10, 0x00, 0x03, 0x00,
  0x00, 0x00, 0x00, 0x58, 0x02, 0x00, 0xC4, 0x0E, 0x00, 0x00, 0xC4, 0x0E, 0x00, 0x00, 0x00, 0x00,
  0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xF8, 0x00, 0x00, 0xE0, 0x07, 0x00, 0x00, 0x1F, 0x00,
  0x00, 0x00
};
// set pin 7 as the slave select for the digital pot:
const int CS = 7;
bool is_header = false;
int mode = 0;
uint8_t start_capture = 0;
#if defined (OV2640_MINI_2MP)
ArduCAM myCAM( OV2640, CS );
#else
ArduCAM myCAM( OV5642, CS );
#endif
uint8_t read_fifo_burst(ArduCAM myCAM);
void setup() {
  // put your setup code here, to run once:
  uint8_t vid, pid;
  uint8_t temp;

  Wire.begin();
  Serial.begin(115200);
  mySerial.begin(115200);

  mySerial.println(F("ACK CMD ArduCAM Start! END"));
  Serial.println(F("ACK CMD ArduCAM Start! END"));
  // set the CS as an output:
  pinMode(CS, OUTPUT);
  digitalWrite(CS, HIGH);
  // initialize SPI:
  SPI.begin();
  //Reset the CPLD
  myCAM.write_reg(0x07, 0x80);
  delay(100);
  myCAM.write_reg(0x07, 0x00);
  delay(100);
  while (1) {
    //Check if the ArduCAM SPI bus is OK
    myCAM.write_reg(ARDUCHIP_TEST1, 0x55);
    temp = myCAM.read_reg(ARDUCHIP_TEST1);
    if (temp != 0x55) {
      mySerial.println(F("ACK CMD SPI interface Error! END"));
      Serial.println(F("ACK CMD SPI interface Error! END"));
      delay(1000); continue;
    } else {
      //      mySerial.println(F("ACK CMD SPI interface OK. END"));
      Serial.println(F("ACK CMD SPI interface OK. END"));
      break;
    }
  }

  while (1) {
    //Check if the camera module type is OV2640
    myCAM.wrSensorReg8_8(0xff, 0x01);
    myCAM.rdSensorReg8_8(OV2640_CHIPID_HIGH, &vid);
    myCAM.rdSensorReg8_8(OV2640_CHIPID_LOW, &pid);
    if ((vid != 0x26 ) && (( pid != 0x41 ) || ( pid != 0x42 ))) {
      mySerial.println(F("ACK CMD Can't find OV2640 module! END"));
      Serial.println(F("ACK CMD Can't find OV2640 module! END"));
      delay(1000); continue;
    }
    else {
      //      mySerial.println(F("ACK CMD OV2640 detected. END"));
      Serial.println(F("ACK CMD OV2640 detected. END"));
      break;
    }
  }
  //Change to JPEG capture mode and initialize the OV5642 module
  myCAM.set_format(JPEG);
  myCAM.InitCAM();
  myCAM.OV2640_set_JPEG_size(OV2640_1600x1200);
  myCAM.OV2640_set_Light_Mode(Auto);
  myCAM.OV2640_set_Special_effects(Normal);
  delay(1000);
  myCAM.clear_fifo_flag();
}
void loop() {
  // put your main code here, to run repeatedly:
  uint8_t temp = 0xff, temp_last = 0;
  bool is_header = false;
  //  if (Serial.available())
  if (mySerial.available())
  {
    //    temp = Serial.read();
    temp = mySerial.read();
    switch (temp)
    {
      case '0':
        start_capture = 1;
        break;
    }
  }
  if (start_capture == 1)
  {
    myCAM.flush_fifo();
    myCAM.clear_fifo_flag();
    //Start capture
    myCAM.start_capture();
    start_capture = 0;
  }
  if (myCAM.get_bit(ARDUCHIP_TRIG, CAP_DONE_MASK))
  {
    //    mySerial.println(F("ACK CMD CAM Capture Done. END"));
    //    Serial.println(F("ACK CMD CAM Capture Done. END"));
    delay(50);
    read_fifo_burst(myCAM);
    //Clear the capture done flag
    myCAM.clear_fifo_flag();
  }



}

uint8_t read_fifo_burst(ArduCAM myCAM)
{
  uint8_t temp = 0, temp_last = 0;
  uint32_t length = 0;
  length = myCAM.read_fifo_length();
  //  mySerial.println(length, DEC);
  //  Serial.println(length, DEC);
  if (length >= MAX_FIFO_SIZE) //512 kb
  {
    //    Serial.println(F("ACK CMD Over size. END"));
    mySerial.println(F("ACK CMD Over size. END"));
    return 0;
  }
  if (length == 0 ) //0 kb
  {
    //    Serial.println(F("ACK CMD Size is 0. END"));
    mySerial.println(F("ACK CMD Size is 0. END"));
    return 0;
  }
  myCAM.CS_LOW();
  myCAM.set_fifo_burst();//Set fifo burst mode
  temp =  SPI.transfer(0x00);
  length --;
  while ( length-- )
  {
    temp_last = temp;
    temp =  SPI.transfer(0x00);
    if (is_header == true)
    {
      mySerial.write(temp);
    }
    else if ((temp == 0xD8) & (temp_last == 0xFF))
    {
      is_header = true;
      //      mySerial.println(F("ACK IMG END"));
      //      Serial.println(F("ACK IMG END"));
      mySerial.write(temp_last);
      mySerial.write(temp);
    }
    if ( (temp == 0xD9) && (temp_last == 0xFF) ) //If find the end ,break while,
      break;
    delayMicroseconds(15);
  }
  myCAM.CS_HIGH();
  is_header = false;
  return 1;
}
