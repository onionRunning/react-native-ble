//
//  Util.h
//  Ble
//
//  Created by 王聪 on 2023/2/17.
//  Copyright © 2023 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>

typedef NS_ENUM(NSUInteger, VersionCompare) {
  VersionCompareNeedUpdate,
  VersionCompareUnNeedUpdate,
  VersionCompareShowMailLogin,
};

typedef void(^Result)(NSArray * _Nullable versions);

NS_ASSUME_NONNULL_BEGIN

@interface Util : NSObject

+ (NSInteger)fetchLengthFromResponse:(NSData *)data;
+ (NSInteger)fetchOrderTypeFromResponse:(NSData *)data;
+ (NSString *)hexString:(NSData *)data;
+ (NSString *)textFrom:(NSString *)hexString;
+ (NSData *)convertHexToData:(NSArray<NSString *> *)command;
+ (NSNumber *)seperatePage:(NSArray<NSString *> *)command;
+ (NSNumber *)seperateIndex:(NSArray<NSString *> *)command;
+ (NSString *)addSpacing:(NSString *)input;

// 版本对比
+ (void)getStoreVersionAndLoaclVersion:(Result)result;

+ (VersionCompare)compareStoreVersion:(NSString *)versionAppStore
                         localVersion:(NSString *)versionLocal;

@end

NS_ASSUME_NONNULL_END
