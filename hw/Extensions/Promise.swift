import PromiseKit

extension Promise {

    static func chain<T, S>(_ datas: [T], _ concurrency: Int, _ iteratorHandle: @escaping (T) -> Promise<S>?) -> Promise<[S]> {
        var generator = datas.makeIterator()
        let iterator = AnyIterator<Promise<S>> {
            guard let next = generator.next() else {
                return nil
            }
            return iteratorHandle(next)
        }
        return when(fulfilled: iterator, concurrently: concurrency)
    }
}
