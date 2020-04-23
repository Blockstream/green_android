//
//  PromiseKitHelper.swift
//  TrueID
//
//  Created by Kittiphat Srilomsak on 3/21/2560 BE.
//  Copyright © 2017 peatiscoding.me all rights reserved.
//
import PromiseKit

extension Promise {

    /**
     * Create a final Promise that chain all delayed promise callback all together.
     */
    static func chain(_ promises:[() -> Promise<T>]) -> Promise<[T]> {
        return Promise<[T]> { seal in
            var out = [T]()

            if promises.count == 0 {
                return seal.fulfill(out)
            }

            let fp: Promise<T>? = promises.reduce(nil) { (r, o) in
                return r?.then { c -> Promise<T> in
                    out.append(c)
                    return o()
                } ?? o()
            }

            fp?.compactMap { c -> Void in
                out.append(c)
                seal.fulfill(out)
            }
            .catch(seal.reject)
        }
    }
}
