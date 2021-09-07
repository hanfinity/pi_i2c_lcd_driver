/**
 * Michael Han 3 September 2021
 * A java library to run LCD displays over I2C on Raspberry Pi
 */
import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.i2c.*;

public class pi_i2c_lcd_driver {

    protected I2C display;
    // constant for i2c bus - (0 for og pi, 1 for rev2+)
    protected static final int I2CBUS = 1;

    // LCD address, often 0x27 can be confirmed with 'sudo i2cdetect -y 1'
    protected static final int address = 0x27;

    // commands
    public static final int LCD_CLEARDISPLAY = 0x01;
    public static final int LCD_RETURNHOME = 0x02;
    public static final int LCD_ENTRYMODESET = 0x04;
    public static final int LCD_DISPLAYCONTROL = 0x08;
    public static final int LCD_CURSORSHIFT = 0x10;
    public static final int LCD_FUNCTIONSET = 0x20;
    public static final int LCD_SETCGRAMADDR = 0x40;
    public static final int LCD_SETDDRAMADDR = 0x80;

    //flags for display entry mode
    public static final int LCD_ENTRYRIGHT = 0x00;
    public static final int LCD_ENTRYLEFT = 0x02;
    public static final int LCD_ENTRYSHIFTINCREMENT = 0x01;
    public static final int LCD_ENTRYSHIFTDECREMENT = 0x00;

    // flags for display on/off control
    public static final int LCD_DISPLAYON = 0x04;
    public static final int LCD_DISPLAYOFF = 0x00;
    public static final int LCD_CURSORON = 0x02;
    public static final int LCD_CURSOROFF = 0x00;
    public static final int LCD_BLINKON = 0x01;
    public static final int LCD_BLINKOFF = 0x00;

    // flags for display/cursor shift
    public static final int LCD_DISPLAYMOVE = 0x08;
    public static final int LCD_CURSORMOVE = 0x00;
    public static final int LCD_MOVERIGHT = 0x04;
    public static final int LCD_MOVELEFT = 0x00;

    // flags for function set
    public static final int LCD_8BITMODE = 0x10;
    public static final int LCD_4BITMODE = 0x00;
    public static final int LCD_2LINE = 0x08;
    public static final int LCD_1LINE = 0x00;
    public static final int LCD_5x10DOTS = 0x04;
    public static final int LCD_5x8DOTS = 0x00;

    // flags for backlight control
    public static final int LCD_BACKLIGHT = 0x08;
    public static final int LCD_NOBACKLIGHT = 0x00;

    public static final int En = 0b00000100; // Enable bit
    public static final int Rw = 0b00000010; // Read/Write bit
    public static final int Rs = 0b00000001; // Register select bit

    public pi_i2c_lcd_driver() {
        Context context = Pi4J.newAutoContext();
        I2CProvider i2CProvider = context.provider("linuxfs-i2c");
        I2CConfig i2CConfig = I2C.newConfigBuilder(context).id("HW-060A").bus(I2CBUS)
                .device(address).build();
        try {
            // initialize the i2c connection on the given address
            this.display = i2CProvider.create(i2CConfig);
            // send commands to initialize display
            for(int i=0; i<4; ++i) {
                lcdWrite((byte)0x03);
            }
            lcdWrite(LCD_FUNCTIONSET | LCD_2LINE | LCD_5x8DOTS | LCD_4BITMODE);
            lcdWrite(LCD_DISPLAYCONTROL | LCD_DISPLAYON);
            lcdWrite(LCD_CLEARDISPLAY);
            lcdWrite(LCD_ENTRYMODESET | LCD_ENTRYLEFT);
            wait(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // clock En bit to latch command
    void lcdStrobe(byte data) {
        try {
            display.write(data | En | LCD_BACKLIGHT);
            wait(5);
            display.write((data & ~En) | LCD_BACKLIGHT);
            wait(1);
        } catch(InterruptedException exc) {
            System.err.println(exc.getMessage());
        }
    }

    // write four bits to the display bus
    void lcdWriteFourBits(byte data) {
        display.write(data | LCD_BACKLIGHT);
        lcdStrobe(data);
    }

    // allow lcdWrite to be used with an int without casting
    void lcdWrite(int cmd) {
        lcdWrite((byte)cmd);
    }

    // write a command to the LCD, mode default to 0
    void lcdWrite(byte cmd) {
        lcdWrite(cmd, 0);
    }

    // write a command to the LCD
    void lcdWrite(byte cmd, int mode) {
        lcdWriteFourBits((byte)(mode | (cmd & 0xF0)));
        lcdWriteFourBits((byte)(mode | ((cmd << 4) & 0xF0)));
    }

    // write a character to LCD, default mode to 1
    void lcdWriteChar(char charVal) {
        lcdWriteChar(charVal, 1);
    }

    // write a character to LCD
    void lcdWriteChar(char charVal, int mode) {
        lcdWriteFourBits((byte)(mode | (charVal & 0xF0)));
        lcdWriteFourBits((byte)(mode | ((charVal << 4) & 0xF0)));
    }

    // put a string with optional positioning
    void lcdDisplayString(String string, int line) {
        lcdDisplayString(string, line, 0);
    }

    void lcdDisplayString(String string) {
        lcdDisplayString(string, 1, 0);
    }
    void lcdDisplayString(String string, int line, int pos) {
        int posNew;
        if(line == 1) posNew = pos;
        else posNew = pos + 0x40;

        lcdWrite(0x80 + posNew);
        for (char c:
             string.toCharArray()) {
            lcdWrite((byte) c, Rs);
        }
    }

    // clear the LCD and return cursor to home
    void lcdClear() {
        lcdWrite(LCD_CLEARDISPLAY);
        lcdWrite(LCD_RETURNHOME);
    }

    // function to set backlight
    void backlight(int setState) {
        if(setState == 1) display.write(LCD_BACKLIGHT);
        else display.write(LCD_NOBACKLIGHT);
    }
}
