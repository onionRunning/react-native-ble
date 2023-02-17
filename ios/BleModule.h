//
//  BleModule.h
//  Ble
//
//  Created by 王聪 on 2023/2/17.
//  Copyright © 2023 Facebook. All rights reserved.
//

#ifndef BleModule_h
#define BleModule_h


#import <Foundation/Foundation.h>
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

NS_ASSUME_NONNULL_BEGIN

@interface BleModule : RCTEventEmitter <RCTBridgeModule>

@end

NS_ASSUME_NONNULL_END

#endif /* BleModule_h */
