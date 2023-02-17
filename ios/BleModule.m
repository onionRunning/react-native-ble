//
//  BleModule.m
//  Ble
//
//  Created by 王聪 on 2023/2/17.
//  Copyright © 2023 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>

#import "BleModule.h"
//#import "BleFile.h"
//#import "Util.h"
#import "BleManager.h"
@interface BleModule() <BleManagerProtocol>

@property (nonatomic, assign) BOOL hasListeners;

@property (nonatomic, copy) RCTPromiseResolveBlock resolve;
@property (nonatomic, copy) RCTPromiseRejectBlock  reject;

@property (nonatomic, strong) NSTimer *timer;
@property (nonatomic, strong) BleManager *bleManager;

@end

@implementation BleModule
RCT_EXPORT_MODULE(BleModule)

+ (BOOL)requiresMainQueueSetup {
  return YES;
}

- (dispatch_queue_t)methodQueue {
  return dispatch_get_main_queue();
}

- (NSArray<NSString *> *)supportedEvents {
  return @[@"EVENT_SCAN_RESULT", @"EVENT_BLE_SWITCHER_CHANGED"];
}

-(void)startObserving {
    self.hasListeners = YES;
}

-(void)stopObserving {
    self.hasListeners = NO;
}

// MARK: export

RCT_EXPORT_METHOD(connectDevice:(NSString *)code :(NSString *)mac :(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject) {
    self.bleManager.deviceCode = code;
    self.resolve = resolve;
    self.reject = reject;
    [self.bleManager connectDevice];
}

RCT_EXPORT_METHOD(isAllOk:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject) {
    self.resolve = resolve;
    self.reject = reject;
    if (self.bleManager == nil) {
        self.bleManager = [BleManager shareInstance];
        self.bleManager.delegate = self;
    } else {
        if (self.bleManager.state == CBManagerStatePoweredOn) {
            [self done:@"is all ok"];
        } else {
            [self fail:@{@"reason":@"you should open ble"}];
        }
    }
}

RCT_EXPORT_METHOD(sendCommandWithCallback:(NSString *)tag
                  :(NSArray<NSString *> *)command
                  :(RCTPromiseResolveBlock)resolve
                  :(RCTPromiseRejectBlock)reject) {
    self.resolve = resolve;
    self.reject = reject;
    self.bleManager.commandTag = tag;
    // NSLog(@"%@ 发送 %@", tag, commandToSend);
    // if (self.timer) {
    //     [self.timer invalidate];
    // }
    // self.timer = [NSTimer scheduledTimerWithTimeInterval:10 target:self selector:@selector(cancelTimer) userInfo:nil repeats:FALSE];
    // [self.bleManager writeperipheralValue:commandToSend];
}


RCT_EXPORT_METHOD(disconnect:(NSString *)sn
                  :(RCTPromiseResolveBlock)resolve
                  :(RCTPromiseRejectBlock)reject) {
    [self.bleManager disconnect];
}

RCT_EXPORT_METHOD(getDevicesConnectState:(NSString *)sn
                  :(RCTPromiseResolveBlock)resolve
                  :(RCTPromiseRejectBlock)reject) {
    BleManager *manager = [BleManager shareInstance];
    resolve([manager getConnectState]);
}



RCT_REMAP_METHOD(requestBlePermission,
                 requestBlePermissionWithResolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject) {
    [[BleManager shareInstance] requestPermission:^(BOOL res) {
        resolve(@(res));
    }];
}

// BleManagerProtocol
- (void)finished:(NSString *)msg {
    [self done:msg];
}

- (void)processFail:(NSDictionary *)dict {
    [self fail:dict];
}

- (void)bleStateChange:(BOOL)isOn {
    if (self.hasListeners) {
        [self sendEventWithName:@"EVENT_BLE_SWITCHER_CHANGED" body:@{@"code": @200, @"data": isOn ? @"STATE_ON" : @"STATE_OFF" }];
    }
}

- (void)requestPermissionSucceed:(BOOL)success {
    if (self.resolve) {
        self.resolve(@(success));
    }
}

// 结果处理
- (void)done:(NSString *)result {
    if (self.resolve) {
//        NSLog(@"%@", result);
        self.resolve(@{@"code": @200, @"data": result});
    }
    self.resolve = nil;
    self.reject = nil;
    [self.bleManager reset];
}

- (void)fail:(NSDictionary *)result {
    if (self.resolve) {
        NSLog(@"%@", result);
        self.resolve(@{@"code": @500, @"data": result});
    }
    self.resolve = nil;
    self.reject = nil;
    [self.bleManager reset];
}

- (void)cancelTimer {
    NSLog(@"超时了！！！");
    [self fail:@{@"reason": @"timeout"}];
}

@end
