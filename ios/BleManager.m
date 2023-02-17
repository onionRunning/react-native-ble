//
//  BleManager.m
//  Ble
//
//  Created by 王聪 on 2023/2/17.
//  Copyright © 2023 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>

#define SERVICE_UUID            @"00010203-0405-0607-0809-0a0b0c0d1910" // 服务的UUID
#define TX_CHARACTERISTIC_UUID  @"00010203-0405-0607-0809-0a0b0c0d2b10" // 读取数据的特征值
#define RX_CHARACTERISTIC_UUID  @"00010203-0405-0607-0809-0a0b0c0d2b11" // 写入命令的特征值

#import "BleManager.h"
//#import "BleFile.h"
#import "Util.h"
//#import "PaintDAO.h"
#import "CRC16.h"
#import <UIKit/UIKit.h>

@interface BleManager() <CBCentralManagerDelegate, CBPeripheralDelegate>

@property (nonatomic, strong) CBCentralManager *centralManager;
@property (nonatomic, strong) CBPeripheral     *peripheral;
@property (nonatomic, strong) CBCharacteristic *rxCharacteristic;
@property (nonatomic, strong) CBCharacteristic *txCharacteristic;

//@property (nonatomic, strong) NSMutableDictionary<NSDictionary *, BleFile *> *dict;
@property (nonatomic, assign) NSInteger resendCounter;
@property (nonatomic, assign) NSInteger retry;
@property (nonatomic, assign) BOOL isRetry;
@property (nonatomic, assign, readwrite) CBManagerState state;

@end

@implementation BleManager
//// MARK: public
+ (instancetype)shareInstance {
  static BleManager *instance = nil;
  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
    instance = [[BleManager alloc] initInPrivate];
  });
  return instance;
}

- (instancetype)initInPrivate {
  self = [super init];
  if (self) {
    _centralManager = [[CBCentralManager alloc] initWithDelegate:self queue:dispatch_get_main_queue()];
  }
  return self;
}

- (instancetype)init {
  return nil;
}

- (instancetype)copy {
  return nil;
}

- (void)reset {
    self.commandTag = nil;
//    self.dict = nil;
    self.retry = 0;
    self.isRetry = false;
}

- (void)connectDevice {
    NSArray<CBPeripheral *> *connectedPeripherals = [self.centralManager retrieveConnectedPeripheralsWithServices:@[[CBUUID UUIDWithString:SERVICE_UUID]]];
    NSDictionary *option = @{ CBCentralManagerScanOptionAllowDuplicatesKey: [NSNumber numberWithBool:FALSE] };
    if (connectedPeripherals.lastObject) {
        self.peripheral = connectedPeripherals.lastObject;
        [self.centralManager connectPeripheral:self.peripheral options:nil];
    } else {
        [self.centralManager scanForPeripheralsWithServices: nil options:option];
    }
}

// 写入数据
- (void)writeperipheralValue:(NSData *)value {
    if(self.rxCharacteristic.properties & CBCharacteristicPropertyWriteWithoutResponse) {
       [self.peripheral writeValue:value forCharacteristic:self.rxCharacteristic type:CBCharacteristicWriteWithoutResponse];
    } else {
       NSLog(@"❤️该字段不能写！");
    }
}

// 断开连接
- (void)disconnect {
    if (self.peripheral) {
        [self.centralManager cancelPeripheralConnection:self.peripheral];
    }
}

- (void)requestPermission:(void (^)(BOOL))success {
    switch (self.centralManager.state) {
        case CBManagerStateUnknown:
            success(NO);
            break;
        case CBManagerStateResetting:
            success(NO);
            break;
        case CBManagerStateUnsupported:
            success(NO);
            break;
        case CBManagerStateUnauthorized:
            [self alertWithTitle:@"Notice"
                         message:@"requires access to your bluetooth to connect the device"
                    confirmTitle:@"Access"
                     cancelTitle:@"Cancel"
                  confirmHandler:^{
                // 去授权app允许访问蓝牙
                [[UIApplication sharedApplication] openURL:[NSURL URLWithString:UIApplicationOpenSettingsURLString] options:@{} completionHandler:^(BOOL success) {
                    
                }];
            }
                   cancelHandler: nil
            ];
            success(NO);
            break;
        case CBManagerStatePoweredOff:
           [self alertWithTitle:@"Notice"
                        message:@"requires open your bluetooth to connect the device"
                   confirmTitle:@"Congirm"
                    cancelTitle:@"Cancel"
                 confirmHandler:nil
                  cancelHandler:nil
           ];
           success(NO);
           break;
        default:
            success(YES);
            break;
    }
}

// 获取连接状态
- (NSString *)getConnectState {
    switch (self.peripheral.state) {
        case CBPeripheralStateConnected:
            return @"STATE_CONNECTED";
        case CBPeripheralStateDisconnecting:
            return  @"STATE_DISCONNECTING";
        case CBPeripheralStateConnecting:
            return  @"STATE_CONNECTING";
        case CBPeripheralStateDisconnected:
            return @"STATE_DISCONNECTED";
    }
}

// MARK: CBCentralManagerDelegate
- (void)centralManagerDidUpdateState:(CBCentralManager *)central {
    self.state = central.state;
    [self.delegate bleStateChange: central.state == CBManagerStatePoweredOn];
    switch (central.state) {
        case CBManagerStatePoweredOn: {
            NSLog(@"❤️蓝牙可用");
            [self.delegate finished:@"is all ok"];
            break;
        }
        default: {
            [self.delegate processFail:@{@"code": @-1001}];
        }
    }
}

// 发现外设
- (void)centralManager:(CBCentralManager *)central didDiscoverPeripheral:(CBPeripheral *)peripheral advertisementData:(NSDictionary<NSString *, id> *)advertisementData RSSI:(NSNumber *)RSSI {
    NSData *data = advertisementData[@"kCBAdvDataManufacturerData"];
    NSString *hexString = [Util hexString:data];
    NSString *code = [Util textFrom:hexString];
    NSLog(@"❤️%@", code);
    if ([code isEqualToString:self.deviceCode]) {
        self.peripheral = peripheral;
        [self.centralManager connectPeripheral:peripheral options:nil];
    }
}
// 连接成功 开始寻找设备服务
- (void)centralManager:(CBCentralManager *)central didConnectPeripheral:(CBPeripheral *)peripheral {
    NSLog(@"❤️连接到名称为（%@）的设备-成功",peripheral.name);
    [self.centralManager stopScan];
    [peripheral setDelegate:self];

    [peripheral discoverServices:@[[CBUUID UUIDWithString:SERVICE_UUID]]];
}

- (void)centralManager:(CBCentralManager *)central didFailToConnectPeripheral:(CBPeripheral *)peripheral error:(NSError *)error {
    NSLog(@"❤️连接到名称为（%@）的设备-失败,原因:%@",[peripheral name],[error localizedDescription]);
}

- (void)centralManager:(CBCentralManager *)central didDisconnectPeripheral:(CBPeripheral *)peripheral error:(NSError *)error {
    NSLog(@"❤️外设连接断开连接 %@ \n", [peripheral name]);
}

// MARK: CBPeripheralDelegate
- (void)peripheral:(CBPeripheral *)peripheral didDiscoverServices:(NSError *)error {
    for (CBService *service in peripheral.services) {
        NSLog(@"❤️所有的服务：%@",service);
    }
    CBService *service = peripheral.services.lastObject;
    [peripheral discoverCharacteristics:nil forService:service];
}

// 发现特征
- (void)peripheral:(CBPeripheral *)peripheral didDiscoverCharacteristicsForService:(CBService *)service error:(NSError *)error {
    for (CBCharacteristic *characteristic in service.characteristics) {
        NSLog(@"❤️service:%@ 的 Characteristic: %@", service.UUID, characteristic.UUID);
        if ([characteristic.UUID isEqual:[CBUUID UUIDWithString:TX_CHARACTERISTIC_UUID]]) { // 订阅通知 CBCharacteristicPropertyRead | CBCharacteristicPropertyNotify
            self.txCharacteristic = characteristic;
            [peripheral setNotifyValue:YES forCharacteristic:characteristic];
        } else if ([characteristic.UUID isEqual:[CBUUID UUIDWithString:RX_CHARACTERISTIC_UUID]]) { // 写入 CBCharacteristicPropertyRead | CBCharacteristicPropertyWriteWithoutResponse
            self.rxCharacteristic = characteristic;
        }
    }
}

// 订阅状态的改变
-(void)peripheral:(CBPeripheral *)peripheral didUpdateNotificationStateForCharacteristic:(CBCharacteristic *)characteristic error:(NSError *)error {
    if (error) {
        NSLog(@"❤️订阅失败 %@",error);
        [self.delegate processFail:@{@"reason": @"订阅失败"}];
    }
    if (characteristic.isNotifying) {
        NSLog(@"❤️订阅成功");
        [self.delegate finished:@"success"];
    } else {
        NSLog(@"❤️取消订阅");
    }
}

// 接收到数据
- (void)peripheral:(CBPeripheral *)peripheral didUpdateValueForCharacteristic:(CBCharacteristic *)characteristic error:(NSError *)error {
    NSLog(@"❤️------------ start ------------");
    // 拿到外设发送过来的数据
    NSData *data = characteristic.value;
    NSString *errorCheck = [Util hexString:data];
    NSLog(@"❤️receive data length %ld", [data length]);
    if ([errorCheck length] <= 8 && [[errorCheck substringWithRange:NSMakeRange(0, 4)] isEqualToString:@"0000"]) {
        NSLog(@"❤️未知错误 ==============>\ncommandTag:%@\nerrorCheck:%@\ndata:%@", self.commandTag, errorCheck, data);
        [self.delegate processFail:@{@"reason": @"未知错误"}];
        NSLog(@"❤️------------ end ------------");
        return;
    }
    if ([errorCheck length] >= 6 && [[errorCheck substringWithRange:NSMakeRange(0, 4)] isEqualToString:@"ffff"]) {
        NSString *errorCode = [errorCheck substringWithRange:NSMakeRange(4, 2)];
        if ([errorCode isEqualToString:@"01"]) {
            [self.delegate processFail:@{@"reason": @"命令错误"}];
        } else if ([errorCode isEqualToString:@"02"]) {
            [self.delegate processFail:@{@"reason": @"校验错误"}];
        } else if ([errorCode isEqualToString:@"03"]) {
            [self.delegate processFail:@{@"reason": @"参数错误"}];
        } else if ([errorCode isEqualToString:@"04"]) {
            [self.delegate processFail:@{@"reason": @"执行超时"}];
        } else {
            [self.delegate processFail:@{@"reason": @"未知错误"}];
        }
        NSLog(@"❤️------------ end ------------");
        return;
    }
    if (self.isRetry) { // 优先判断重传
        [self retryReceive:data];
    } else if ([self.commandTag isEqualToString: @"TAG_COMMAND_READ_SYNC_DATA_NEW"]) { // 图片、轨迹、同步数据
        [self processData:data type:OrderTypeSync];
    } else if ([self.commandTag isEqualToString: @"TAG_COMMAND_READ_PAINT_TRACE"]) {
        [self processData:data type:OrderTypeImageAndTrack];

    } else if ([self.commandTag isEqualToString: @"TAG_COMMAND_DEVICE_INFO"]
               || [self.commandTag isEqualToString: @"TAG_COMMAND_DEVICE_SERIAL_NUMBER"]
               || [self.commandTag isEqualToString: @"TAG_COMMAND_FIRMWARE_INFO"]
               || [self.commandTag isEqualToString: @"TAG_COMMAND_HARDWARE_INFO"]
               || [self.commandTag isEqualToString: @"TAG_COMMAND_BOOT1_VERSION"]
               || [self.commandTag isEqualToString: @"TAG_COMMAND_BOOT2_VERSION"]) {
        NSLog(@"❤️%@ 接收到数据回调%@", self.commandTag, data);
        NSData *oderData = [data subdataWithRange:NSMakeRange(4, 1)];
        unsigned order = 0;
        NSScanner *scanner = [NSScanner scannerWithString:[Util hexString:oderData]];
        [scanner scanHexInt:&order];
        
        NSData *featureData = [data subdataWithRange:NSMakeRange(5, 1)];
        unsigned feature = 0;
        scanner = [NSScanner scannerWithString:[Util hexString:featureData]];
        [scanner scanHexInt:&feature];
        if (order == 130) {
            NSString *result = [Util hexString:data];
            [self.delegate finished:result];
        }
    } else if ([self.commandTag isEqualToString: @"TAG_COMMAND_DEVICE_UPGRADE_MODE"] || [self.commandTag isEqualToString: @"TAG_COMMAND_REBOOT_DEVICE"]) {
        NSString *result = [Util hexString:data];
        NSString *spacing = [Util addSpacing:result];
        [self.delegate finished:spacing];
    }
    NSLog(@"❤️------------ end ------------");
}

// 写入数据
- (void)peripheral:(CBPeripheral *)peripheral didWriteValueForCharacteristic:(nonnull CBCharacteristic *)characteristic error:(nullable NSError *)error {
    if (error) {
        NSLog(@"❤️写入失败 %@", error);
    } else {
        NSLog(@"❤️写入成功 %@", characteristic.value);
    }
}

// MARK: helper

- (void)processData:(NSData *)data type:(OrderType)orderType {
    NSData *typeData = [data subdataWithRange:NSMakeRange(5, 1)];
    unsigned type = 0;
    NSScanner *scanner = [NSScanner scannerWithString:[Util hexString:typeData]];
    [scanner scanHexInt:&type];
    BOOL isLast = NO;
    if (type == 0 && orderType == OrderTypeSync) {
        [self.delegate finished:@""];
        return;
    } else if (type == 2) {
        isLast = YES;
    }
    NSInteger length = [Util fetchLengthFromResponse:data];
    
    NSData *frameIndexData = [data subdataWithRange:NSMakeRange(6, 2)];
    unsigned frameIndex = 0;
    scanner = [NSScanner scannerWithString:[Util hexString:frameIndexData]];
    [scanner scanHexInt:&frameIndex];
    
    if (length != [data length] - 6) {
        [self.delegate processFail:@{@"reason": @"数据长度对不上啊"}];
        return;
    }
    NSLog(@"❤️%@ 接收到数据回调 %@最后一个 长度%ld 第%d帧 %@", self.commandTag, isLast ? @"是" : @"不是", length, frameIndex, data);
//    if (!self.dict) {
//        self.dict = [NSMutableDictionary dictionary];
//    }
//    BleFile *fileData = [[BleFile alloc] init];
//    fileData.data = [data subdataWithRange:NSMakeRange(8, length - 4)]; // 从命令位起算 不含校验位 去除命令位、功能位、数据索引-4
//    fileData.index = frameIndex;
//    [self.dict setValue:fileData forKey:[NSString stringWithFormat:@"%d", frameIndex]];
//    if (isLast) {
//        [self checkDataAndResend:orderType];
//    }
}

- (void)retryReceive:(NSData *)data {
    NSData *frameIndexData = [data subdataWithRange:NSMakeRange(5, 2)]; // 0x cc cc 00 03 86 (00 00)
    NSInteger length = [Util fetchLengthFromResponse:data];
    
    unsigned frameIndex = 0;
    NSScanner *scanner = [NSScanner scannerWithString:[Util hexString:frameIndexData]];
    [scanner scanHexInt:&frameIndex];
    NSLog(@"❤️%@ 接收到重传 长度%ld 第%d帧 %@", self.commandTag, length, frameIndex, data);
//    BleFile *fileData = [[BleFile alloc] init];
//    if (length <= 2) {
//        fileData.data = [[NSData alloc] init];
//    } else {
//        fileData.data = [data subdataWithRange:NSMakeRange(7, length - 3)]; // 从命令位起算 不含校验位 去除命令位、数据索引-3
//    }
//
//    fileData.index = frameIndex;
//    [self.dict setValue:fileData forKey:[NSString stringWithFormat:@"%d", frameIndex]];
//
//    self.resendCounter -= 1;
//    if (self.resendCounter == 0) {
//        NSLog(@"❤️%@ 重传开始", self.commandTag);
//        [self checkDataAndResend:OrderTypeNone];
//    }
}

- (void)checkDataAndResend:(OrderType)type {
    if (self.retry >= 10) {
        [self.delegate processFail:@{@"reason": @"retry too many times"}];
        return;
    }
//    NSArray *files = [self.dict allValues];
//    NSArray<BleFile *> *sorted = [files sortedArrayUsingComparator:^NSComparisonResult(BleFile *obj1, BleFile *obj2) {
//        return obj1.index > obj2.index;
//    }];
//    if ([sorted lastObject].index != [sorted count] - 1 && [sorted count] > 0) {
//        self.retry++;
//        self.isRetry = true;
//        NSMutableArray *temp = [NSMutableArray array];
//        for (int i = 0; i <= [sorted lastObject].index ; i++) {
//            [temp addObject:@(i)];
//        }
//        for (int i = 0; i <= [sorted count] - 1; i++) {
//            [temp removeObject:@(sorted[i].index)];
//        }
//        self.resendCounter = temp.count;
//        NSString *order = @"00";
//        if (type == OrderTypeSync) {
//            order = @"04";
//        } else if (type == OrderTypeImageAndTrack) {
//            order = @"0b";
//        }
//        NSMutableData *totalData = [NSMutableData data];
//        for (NSNumber *i in temp) {
//            NSMutableArray<NSString *> *command = [@[@"cc", @"cc", @"00", @"03", order] mutableCopy];
//            NSString *index = [[NSString stringWithFormat:@"%02X", i.unsignedIntValue] lowercaseString];
//            if ([index length] != 4) {
//                NSString *preZero = [@"" stringByPaddingToLength:4 - [index length] withString: @"0" startingAtIndex:0];
//                index = [preZero stringByAppendingString:index];
//            }
//            NSString *pageHigh = [index substringWithRange:NSMakeRange(0, 2)];
//            NSString *pageLow = [index substringWithRange:NSMakeRange(2, 2)];
//            [command addObject:pageHigh];
//            [command addObject:pageLow];
//            
//            NSData *data = [Util convertHexToData:command];
//            uint8_t *buffer = (uint8_t *)[data bytes];
//            uint16_t crc16 = CRC_16B(buffer, [data length]);
//            NSString *crc16String = [[NSString stringWithFormat:@"%02X", crc16] lowercaseString];
//            if ([crc16String length] < 4) {
//                NSString *preZero = [@"" stringByPaddingToLength:4 - [crc16String length] withString: @"0" startingAtIndex:0];
//                crc16String = [preZero stringByAppendingString:crc16String];
//            }
//            NSString *high = [crc16String substringWithRange:NSMakeRange(0, 2)];
//            NSString *low = [crc16String substringWithRange:NSMakeRange(2, 2)];
//            [command addObject:high];
//            [command addObject:low];
//            
//            NSData *commandToSend = [Util convertHexToData:command];
//            [totalData appendData:commandToSend];
//        }
//        NSLog(@"❤️%@ 发送 %@", self.commandTag, totalData);
//        [self writeperipheralValue:totalData];
//    } else {
//        self.isRetry = false;
//        NSMutableData *data = [NSMutableData data];
//        for (BleFile *file in sorted) {
//            [data appendData:file.data];
//        }
//        NSString *result = [Util hexString:data];
//        if ([result length] == 0) {
//            [self.delegate processFail:@{@"reason": @"数据长度为0"}];
//        }
//        if ([self.commandTag isEqualToString:@"TAG_COMMAND_READ_PAINT_TRACE"]) {
//            NSString *pix = [result substringWithRange:NSMakeRange(0, 928 * 2)];
//            NSString *dat = [result substringWithRange:NSMakeRange(928 * 2, [result length] - 928 * 2)];
//            [[PaintDAO shared] insert:self.deviceCode page:self.page index:self.index paintContent:pix traceContent:dat];
//            [self.delegate finished:@""];
//        } else {
//            NSString *spacing = [Util addSpacing:result];
//            [self.delegate finished:spacing];
//        }
//        
//    }
}

- (void)alertWithTitle:(NSString*)title
               message:(NSString*)message
          confirmTitle:(NSString *)confirmTitle
           cancelTitle:(NSString *)cancelTitle
        confirmHandler:(void(^)(void))confirmHandler
         cancelHandler:(void(^)(void))cancelHandler {
    dispatch_async(dispatch_get_main_queue(), ^{
        UIAlertController *alertController = [UIAlertController alertControllerWithTitle:title
                                                                                 message:message
                                                                          preferredStyle:UIAlertControllerStyleAlert];
        UIAlertAction *confirmAction = [UIAlertAction actionWithTitle:confirmTitle style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
            confirmHandler();
        }];
        UIAlertAction *cancelAction = [UIAlertAction actionWithTitle:cancelTitle style:UIAlertActionStyleCancel handler:^(UIAlertAction * _Nonnull action) {
            cancelHandler();
        }];
        [alertController addAction:confirmAction];
        [alertController addAction:cancelAction];
        id rootViewController = [UIApplication sharedApplication].delegate.window.rootViewController;
        if([rootViewController isKindOfClass:[UINavigationController class]]) {
            rootViewController = ((UINavigationController *)rootViewController).viewControllers.firstObject;
        }
        if([rootViewController isKindOfClass:[UITabBarController class]]) {
            rootViewController = ((UITabBarController *)rootViewController).selectedViewController;
        }
        [rootViewController presentViewController:alertController animated:YES completion:nil];
    });
}

@end
