//
//  BleManager.h
//  Ble
//
//  Created by 王聪 on 2023/2/17.
//  Copyright © 2023 Facebook. All rights reserved.
//

#ifndef BleManager_h
#define BleManager_h


#import <Foundation/Foundation.h>
#import <CoreBluetooth/CoreBluetooth.h>

NS_ASSUME_NONNULL_BEGIN

typedef NS_ENUM(NSUInteger, OrderType) {
  OrderTypeNone,
  OrderTypeSync, // 命令0a 响应8a
  OrderTypeImageAndTrack, // 命令0b 响应8b
};

@protocol BleManagerProtocol <NSObject>

- (void)finished:(NSString *)msg;
- (void)processFail:(NSDictionary *)dict;
- (void)bleStateChange:(BOOL)isOn;
- (void)requestPermissionSucceed:(BOOL)success;

@end

@interface BleManager : NSObject

@property (nonatomic, weak) id<BleManagerProtocol> delegate;
@property (nonatomic, copy) NSString *deviceCode;
@property (nonatomic, strong) NSNumber *page;
@property (nonatomic, strong) NSNumber *index;
@property (nonatomic, copy, nullable) NSString *commandTag;
@property (nonatomic, assign, readonly) CBManagerState state;

+ (instancetype)shareInstance;

- (void)connectDevice;
- (void)reset;
- (void)writeperipheralValue:(NSData *)value;
- (void)disconnect;
- (NSString *)getConnectState;
- (void)requestPermission:(void (^)(BOOL))success;
@end

NS_ASSUME_NONNULL_END

#endif /* BleManager_h */
