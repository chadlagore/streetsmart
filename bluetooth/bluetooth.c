#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include "bluetooth.h"


void init_bluetooth(void){
	BLUETOOTH_CONTROL = 0x15;
	BLUETOOTH_BAUD = 0x01; //datasheet recommends 115k baud rate
}

void sendchar_bluetooth(char c){
    while(1) {
			if (BLUETOOTH_STATUS & BLUETOOTH_TX_MASK) {
				BLUETOOTH_TXDATA = c;
				break;
			}
		}
}

char get_char(void){
	if (BLUETOOTH_STATUS & BLUETOOTH_RX_MASK) {
		return BLUETOOTH_RXDATA;
	}
	else {
		return '-';
	}
}

char polling_char(void) {
	while (!(BLUETOOTH_STATUS & BLUETOOTH_RX_MASK));

	return BLUETOOTH_RXDATA;
}

void receive(char incoming[]) {

	int i = 0;
	int length = strlen(incoming);

	while (i < length) {
		if ((incoming[i] = get_char()) != '-') {

			printf("%c", incoming[i]);

			if (incoming[i] == '\0' || incoming[i] == '?') {
				printf("done");
				break;
			}
			usleep(100000);
			i++;
		}
	}
	//debug print
	incoming[i] = '\0';
}

void sendstring_bluetooth(char str[]){

  int i;
	int length = strlen(str);

    for (i = 0; i < length && str[i] != '\0'; i++) {
    	usleep(100000); //100ms wait
			sendchar_bluetooth(str[i]);
	}
}

void command_mode(void){
  printf("Entering Command Mode...\n");
	usleep(100000); // 1s wait
	sendstring_bluetooth("$$$");
	usleep(100000);

//	wait_for_read();
//	char O = get_char();
//	printf("%c", O);
//	wait_for_read();
//	char K = get_char();
//	printf("%c\n", K);
}

void data_mode(void) {
  printf("Entering Data Mode...\n");
	usleep(100000); // 1s wait
	sendstring_bluetooth("---\r\n");
	usleep(100000);
}

void reset_bluetooth(void) {
	command_mode();
	sendstring_bluetooth("SF,");
	sendstring_bluetooth("1\r\n");
	data_mode();
}

void set_name(char name[]) {
	command_mode();
	sendstring_bluetooth("SN,");
	sendstring_bluetooth(name);
	sendstring_bluetooth("\r\n");
	data_mode();
}

void set_pw(char pw[]) {
	command_mode();
	sendstring_bluetooth("SP,");
	sendstring_bluetooth(pw);
	sendstring_bluetooth("\r\n");
	data_mode();
}

void test_bluetooth(void) {
    printf("Testing Bluetooth...\n");
    printf("Connection established...\n");

    const char btname[] = "bluetooth_is_suffering";
    const char password[] = "morning";

    set_name(btname);
    set_pw(password);

    printf("Bluetooth successfully updated!\n");
}
