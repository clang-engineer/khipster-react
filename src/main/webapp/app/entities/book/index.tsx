import React from 'react';
import { Route } from 'react-router-dom';

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';

import Book from './book';
import BookDetail from './book-detail';
import BookUpdate from './book-update';
import BookDeleteDialog from './book-delete-dialog';

const BookRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route index element={<Book />} />
    <Route path="new" element={<BookUpdate />} />
    <Route path=":id">
      <Route index element={<BookDetail />} />
      <Route path="edit" element={<BookUpdate />} />
      <Route path="delete" element={<BookDeleteDialog />} />
    </Route>
  </ErrorBoundaryRoutes>
);

export default BookRoutes;
