package realTcpServer.connections

import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.implicits._
import scala.collection.immutable.HashMap

final class ConnectionsCache[F[_]: Sync](cache: Ref[F, HashMap[String, Connected[F]]]) {
  def getConnection(id: String): F[Option[Connected[F]]]        = cache.get.map(_.get(id))
  def registerConnection(connection: Connected[F]): F[Unit]     = cache.update(_ + (connection.id -> connection))
  def unregisterConnection(id: String): F[Option[Connected[F]]] = cache.modify(old => (old - id, old.get(id)))
}

object ConnectionsCache {
  def apply[F[_]: Sync]: F[ConnectionsCache[F]] =
    Ref[F].of(HashMap.empty[String, Connected[F]]).map(cache => new ConnectionsCache[F](cache))
}
