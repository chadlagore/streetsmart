#ifndef _BLUETOOTH_H_
#define _BLUETOOTH_H_

/* Define bluetooth control registers */
#define BLUETOOTH_STATUS   (*(volatile unsigned char *)(0x84000220))
#define BLUETOOTH_CONTROL  (*(volatile unsigned char *)(0x84000220))
#define BLUETOOTH_TXDATA   (*(volatile unsigned char *)(0x84000222))
#define BLUETOOTH_RXDATA   (*(volatile unsigned char *)(0x84000222))
#define BLUETOOTH_BAUD     (*(volatile unsigned char *)(0x84000224))

#define BLUETOOTH_TX_MASK 0x02
#define BLUETOOTH_RX_MASK 0x01

void init_bluetooth(void);
void sendchar_bluetooth(char c);
char get_char(void);
char polling_char(void);
void receive(char incoming[]);
void sendstring_bluetooth(char str[]);
void command_mode(void);
void data_mode(void);

void reset_bluetooth(void);
void set_name(char name[]);
void set_pw(char pw[]);
void test_bluetooth();

#endif /* _BLUETOOTH_H_ */
