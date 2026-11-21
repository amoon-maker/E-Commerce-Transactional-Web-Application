import { useCallback, useEffect, useState } from "react";
import { Button, Col, Form, Row, Table } from "react-bootstrap";

type Product = {
  productId: number;
  vendorId: number;
  sku: string;
  productName: string;
  description: string | null;
  price: number;
  salePrice: number | null;
  packageSize: number | null;
  packageUnit: string | null;
  stockQuantity: number;
  reservedQuantity: number;
  reorderLevel: number;
  active: boolean;
  status: string;
};

type Vendor = {
  vendorId: number;
  businessName: string;
  contactName: string | null;
  email: string | null;
  phone: string | null;
  addressLine1: string | null;
  city: string | null;
  emirate: string | null;
  country: string;
  active: boolean;
};

type ProductDraft = {
  vendorId: string;
  sku: string;
  productName: string;
  description: string;
  price: string;
  salePrice: string;
  packageSize: string;
  packageUnit: string;
  stockQuantity: string;
  reorderLevel: string;
  active: boolean;
};

type VendorDraft = {
  businessName: string;
  contactName: string;
  email: string;
  phone: string;
  addressLine1: string;
  city: string;
  emirate: string;
  country: string;
  active: boolean;
};

type CatalogManagementPanelProps = {
  sectionId: string;
  isAdmin: boolean;
  vendorId?: number;
  apiRequest: <T>(path: string, options?: RequestInit) => Promise<T>;
  onProductsChanged: () => void;
  onClose: () => void;
};

const newProductDraft = (vendorId = ""): ProductDraft => ({
  vendorId,
  sku: "",
  productName: "",
  description: "",
  price: "",
  salePrice: "",
  packageSize: "",
  packageUnit: "",
  stockQuantity: "0",
  reorderLevel: "0",
  active: true,
});

const newVendorDraft = (): VendorDraft => ({
  businessName: "",
  contactName: "",
  email: "",
  phone: "",
  addressLine1: "",
  city: "",
  emirate: "",
  country: "United Arab Emirates",
  active: true,
});

function CatalogManagementPanel({
  sectionId,
  isAdmin,
  vendorId,
  apiRequest,
  onProductsChanged,
  onClose,
}: CatalogManagementPanelProps) {
  const [products, setProducts] = useState<Product[]>([]);
  const [vendors, setVendors] = useState<Vendor[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [editingProductId, setEditingProductId] = useState<number | null>(null);
  const [productDraft, setProductDraft] = useState<ProductDraft>(() =>
    newProductDraft(vendorId ? String(vendorId) : ""),
  );
  const [editingVendorId, setEditingVendorId] = useState<number | null>(null);
  const [vendorDraft, setVendorDraft] = useState<VendorDraft>(newVendorDraft);

  const loadManagementData = useCallback(async () => {
    const productData = await apiRequest<Product[]>("/api/products");
    const vendorData = isAdmin ? await apiRequest<Vendor[]>("/api/vendors") : [];
    return { productData, vendorData };
  }, [apiRequest, isAdmin]);

  const applyManagementData = useCallback(
    (data: { productData: Product[]; vendorData: Vendor[] }) => {
      setProducts(data.productData);
      if (isAdmin) {
        setVendors(data.vendorData);
        setProductDraft((current) => current.vendorId || !data.vendorData.length
          ? current
          : { ...current, vendorId: String(data.vendorData[0].vendorId) });
      }
    },
    [isAdmin],
  );

  const refreshData = useCallback(async () => {
    try {
      const data = await loadManagementData();
      setError("");
      applyManagementData(data);
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to load management data.",
      );
    } finally {
      setLoading(false);
    }
  }, [applyManagementData, loadManagementData]);

  useEffect(() => {
    let active = true;
    loadManagementData()
      .then((data) => {
        if (!active) return;
        setError("");
        applyManagementData(data);
      })
      .catch((requestError: unknown) => {
        if (!active) return;
        setError(
          requestError instanceof Error
            ? requestError.message
            : "Unable to load management data.",
        );
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, [applyManagementData, loadManagementData]);

  const visibleProducts = products.filter(
    (product) => isAdmin || product.vendorId === vendorId,
  );

  function startProductEdit(product: Product) {
    setEditingProductId(product.productId);
    setProductDraft({
      vendorId: String(product.vendorId),
      sku: product.sku,
      productName: product.productName,
      description: product.description ?? "",
      price: String(product.price),
      salePrice: product.salePrice == null ? "" : String(product.salePrice),
      packageSize: product.packageSize == null ? "" : String(product.packageSize),
      packageUnit: product.packageUnit ?? "",
      stockQuantity: String(product.stockQuantity),
      reorderLevel: String(product.reorderLevel),
      active: product.active,
    });
    setError("");
    setNotice("");
  }

  function resetProductForm() {
    setEditingProductId(null);
    setProductDraft(newProductDraft(vendorId ? String(vendorId) : String(vendors[0]?.vendorId ?? "")));
  }

  async function saveProduct(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError("");
    setNotice("");

    const body = {
      vendorId: Number(productDraft.vendorId),
      sku: productDraft.sku.trim(),
      productName: productDraft.productName.trim(),
      description: productDraft.description.trim() || null,
      price: Number(productDraft.price),
      salePrice: productDraft.salePrice === "" ? null : Number(productDraft.salePrice),
      packageSize: productDraft.packageSize === "" ? null : Number(productDraft.packageSize),
      packageUnit: productDraft.packageUnit.trim() || null,
      stockQuantity: Number(productDraft.stockQuantity),
      reorderLevel: Number(productDraft.reorderLevel),
      active: productDraft.active,
      // Omitting variants leaves any existing product variants unchanged.
    };

    try {
      if (editingProductId == null) {
        await apiRequest<Product>("/api/products", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body),
        });
        setNotice("Product added.");
      } else {
        await apiRequest<Product>(`/api/products/${editingProductId}`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body),
        });
        setNotice("Product updated.");
      }
      resetProductForm();
      onProductsChanged();
      await refreshData();
    } catch (requestError) {
      setError(
        requestError instanceof Error ? requestError.message : "Unable to save product.",
      );
    } finally {
      setSaving(false);
    }
  }

  async function deleteProduct(product: Product) {
    if (!window.confirm(`Delete ${product.productName} (${product.sku})?`)) return;
    setError("");
    setNotice("");
    try {
      await apiRequest<void>(`/api/products/${product.productId}`, { method: "DELETE" });
      setNotice("Product deleted.");
      if (editingProductId === product.productId) resetProductForm();
      onProductsChanged();
      await refreshData();
    } catch (requestError) {
      setError(
        requestError instanceof Error ? requestError.message : "Unable to delete product.",
      );
    }
  }

  function startVendorEdit(vendor: Vendor) {
    setEditingVendorId(vendor.vendorId);
    setVendorDraft({
      businessName: vendor.businessName,
      contactName: vendor.contactName ?? "",
      email: vendor.email ?? "",
      phone: vendor.phone ?? "",
      addressLine1: vendor.addressLine1 ?? "",
      city: vendor.city ?? "",
      emirate: vendor.emirate ?? "",
      country: vendor.country,
      active: vendor.active,
    });
    setError("");
    setNotice("");
  }

  async function saveVendor(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError("");
    setNotice("");
    try {
      await apiRequest<Vendor>(
        editingVendorId == null ? "/api/vendors" : `/api/vendors/${editingVendorId}`,
        {
          method: editingVendorId == null ? "POST" : "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            ...vendorDraft,
            contactName: vendorDraft.contactName.trim() || null,
            email: vendorDraft.email.trim() || null,
            phone: vendorDraft.phone.trim() || null,
            addressLine1: vendorDraft.addressLine1.trim() || null,
            city: vendorDraft.city.trim() || null,
            emirate: vendorDraft.emirate.trim() || null,
          }),
        },
      );
      setNotice(editingVendorId == null ? "Vendor added." : "Vendor updated.");
      setEditingVendorId(null);
      setVendorDraft(newVendorDraft());
      await refreshData();
    } catch (requestError) {
      setError(
        requestError instanceof Error ? requestError.message : "Unable to save vendor.",
      );
    } finally {
      setSaving(false);
    }
  }

  async function deleteVendor(vendor: Vendor) {
    if (!window.confirm(`Delete vendor ${vendor.businessName}?`)) return;
    setError("");
    setNotice("");
    try {
      await apiRequest<void>(`/api/vendors/${vendor.vendorId}`, { method: "DELETE" });
      setNotice("Vendor deleted.");
      if (editingVendorId === vendor.vendorId) {
        setEditingVendorId(null);
        setVendorDraft(newVendorDraft());
      }
      await refreshData();
    } catch (requestError) {
      setError(
        requestError instanceof Error ? requestError.message : "Unable to delete vendor.",
      );
    }
  }

  return (
    <section id={sectionId} className="cart-section navigation-scroll-target" aria-label={isAdmin ? "Admin management" : "Vendor workspace"}>
      <div className="container">
        <div className="cart-panel">
          <div className="cart-heading">
            <div>
              <span className="eyebrow">{isAdmin ? "ADMINISTRATION" : "YOUR CATALOG"}</span>
              <h2>{isAdmin ? "Admin Management" : "Vendor Workspace"}</h2>
              <p className="mb-0">
                {isAdmin
                  ? "Manage vendors and products. Order operations are available in Vendor Orders."
                  : "Manage products assigned to your vendor account. Order operations are available in Vendor Orders."}
              </p>
            </div>
            <div className="d-flex gap-2">
              <Button variant="outline-secondary" onClick={() => { setLoading(true); setError(""); void refreshData(); }} disabled={loading}>
                Refresh
              </Button>
              <Button variant="outline-secondary" onClick={onClose}>Close</Button>
            </div>
          </div>

          {error && <div className="alert alert-danger" role="alert">{error}</div>}
          {notice && <div className="alert alert-success" role="status">{notice}</div>}
          {!isAdmin && !vendorId && (
            <div className="alert alert-danger" role="alert">
              This account is not linked to a vendor. Contact an administrator.
            </div>
          )}

          {isAdmin && (
            <>
              <h3 className="mt-4">Vendors</h3>
              {loading ? <p>Loading vendors...</p> : (
                <Table responsive bordered hover size="sm">
                  <thead><tr><th>Business</th><th>Email</th><th>Country</th><th>Status</th><th>Actions</th></tr></thead>
                  <tbody>
                    {vendors.map((vendor) => (
                      <tr key={vendor.vendorId}>
                        <td>{vendor.businessName}</td><td>{vendor.email ?? "—"}</td>
                        <td>{vendor.country}</td><td>{vendor.active ? "Active" : "Inactive"}</td>
                        <td className="text-nowrap">
                          <Button size="sm" variant="outline-primary" onClick={() => startVendorEdit(vendor)}>Edit</Button>{" "}
                          <Button size="sm" variant="outline-danger" onClick={() => void deleteVendor(vendor)}>Delete</Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
              )}

              <h4 className="mt-4">{editingVendorId == null ? "Add vendor" : "Edit vendor"}</h4>
              <p className="text-muted">
                Vendor records do not create login accounts; vendor user accounts are provisioned separately.
              </p>
              <Form onSubmit={saveVendor}>
                <Row className="g-3">
                  <Col md={6}><Form.Label>Business name</Form.Label><Form.Control required maxLength={150} value={vendorDraft.businessName} onChange={(event) => setVendorDraft({ ...vendorDraft, businessName: event.target.value })} /></Col>
                  <Col md={6}><Form.Label>Contact name</Form.Label><Form.Control maxLength={120} value={vendorDraft.contactName} onChange={(event) => setVendorDraft({ ...vendorDraft, contactName: event.target.value })} /></Col>
                  <Col md={6}><Form.Label>Email</Form.Label><Form.Control type="email" maxLength={255} value={vendorDraft.email} onChange={(event) => setVendorDraft({ ...vendorDraft, email: event.target.value })} /></Col>
                  <Col md={6}><Form.Label>Phone</Form.Label><Form.Control maxLength={30} value={vendorDraft.phone} onChange={(event) => setVendorDraft({ ...vendorDraft, phone: event.target.value })} /></Col>
                  <Col md={6}><Form.Label>Address</Form.Label><Form.Control maxLength={200} value={vendorDraft.addressLine1} onChange={(event) => setVendorDraft({ ...vendorDraft, addressLine1: event.target.value })} /></Col>
                  <Col md={3}><Form.Label>City</Form.Label><Form.Control maxLength={80} value={vendorDraft.city} onChange={(event) => setVendorDraft({ ...vendorDraft, city: event.target.value })} /></Col>
                  <Col md={3}><Form.Label>Emirate</Form.Label><Form.Control maxLength={80} value={vendorDraft.emirate} onChange={(event) => setVendorDraft({ ...vendorDraft, emirate: event.target.value })} /></Col>
                  <Col md={6}><Form.Label>Country</Form.Label><Form.Control required maxLength={80} value={vendorDraft.country} onChange={(event) => setVendorDraft({ ...vendorDraft, country: event.target.value })} /></Col>
                  <Col md={6} className="d-flex align-items-end"><Form.Check type="switch" label="Active" checked={vendorDraft.active} onChange={(event) => setVendorDraft({ ...vendorDraft, active: event.target.checked })} /></Col>
                </Row>
                <div className="mt-3 d-flex gap-2">
                  <Button type="submit" disabled={saving}>{saving ? "Saving..." : editingVendorId == null ? "Add vendor" : "Save vendor"}</Button>
                  {editingVendorId != null && <Button type="button" variant="outline-secondary" onClick={() => { setEditingVendorId(null); setVendorDraft(newVendorDraft()); }}>Cancel</Button>}
                </div>
              </Form>
            </>
          )}

          <h3 className="mt-4">Products</h3>
          {loading ? <p>Loading products...</p> : visibleProducts.length === 0 ? <p>No products are assigned to this account.</p> : (
            <Table responsive bordered hover size="sm">
              <thead><tr><th>SKU</th><th>Product</th>{isAdmin && <th>Vendor</th>}<th>Price</th><th>Stock</th><th>Status</th><th>Actions</th></tr></thead>
              <tbody>
                {visibleProducts.map((product) => (
                  <tr key={product.productId}>
                    <td>{product.sku}</td><td>{product.productName}</td>{isAdmin && <td>{vendors.find((vendor) => vendor.vendorId === product.vendorId)?.businessName ?? `#${product.vendorId}`}</td>}
                    <td>AED {Number(product.salePrice ?? product.price).toFixed(2)}</td>
                    <td>{product.stockQuantity} ({product.reservedQuantity} reserved)</td>
                    <td>{product.active ? product.status : "Inactive"}</td>
                    <td className="text-nowrap">
                      <Button size="sm" variant="outline-primary" onClick={() => startProductEdit(product)}>Edit</Button>{" "}
                      <Button size="sm" variant="outline-danger" onClick={() => void deleteProduct(product)}>Delete</Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          )}

          <h4 className="mt-4">{editingProductId == null ? "Add product" : "Edit product"}</h4>
          <Form onSubmit={saveProduct}>
            <Row className="g-3">
              {isAdmin && (
                <Col md={6}><Form.Label>Vendor</Form.Label><Form.Select required value={productDraft.vendorId} onChange={(event) => setProductDraft({ ...productDraft, vendorId: event.target.value })}><option value="">Select a vendor</option>{vendors.map((vendor) => <option key={vendor.vendorId} value={vendor.vendorId}>{vendor.businessName}</option>)}</Form.Select></Col>
              )}
              <Col md={isAdmin ? 6 : 4}><Form.Label>SKU</Form.Label><Form.Control required maxLength={60} value={productDraft.sku} onChange={(event) => setProductDraft({ ...productDraft, sku: event.target.value })} /></Col>
              <Col md={isAdmin ? 6 : 4}><Form.Label>Product name</Form.Label><Form.Control required maxLength={180} value={productDraft.productName} onChange={(event) => setProductDraft({ ...productDraft, productName: event.target.value })} /></Col>
              <Col md={4}><Form.Label>Price (AED)</Form.Label><Form.Control required type="number" min="0" step="0.01" value={productDraft.price} onChange={(event) => setProductDraft({ ...productDraft, price: event.target.value })} /></Col>
              <Col md={4}><Form.Label>Sale price (optional)</Form.Label><Form.Control type="number" min="0" step="0.01" value={productDraft.salePrice} onChange={(event) => setProductDraft({ ...productDraft, salePrice: event.target.value })} /></Col>
              <Col md={4}><Form.Label>Stock quantity</Form.Label><Form.Control required type="number" min="0" step="1" value={productDraft.stockQuantity} onChange={(event) => setProductDraft({ ...productDraft, stockQuantity: event.target.value })} /></Col>
              <Col md={4}><Form.Label>Reorder level</Form.Label><Form.Control required type="number" min="0" step="1" value={productDraft.reorderLevel} onChange={(event) => setProductDraft({ ...productDraft, reorderLevel: event.target.value })} /></Col>
              <Col md={4}><Form.Label>Package size</Form.Label><Form.Control type="number" min="0.01" step="0.01" value={productDraft.packageSize} onChange={(event) => setProductDraft({ ...productDraft, packageSize: event.target.value })} /></Col>
              <Col md={4}><Form.Label>Package unit</Form.Label><Form.Control maxLength={20} value={productDraft.packageUnit} onChange={(event) => setProductDraft({ ...productDraft, packageUnit: event.target.value })} /></Col>
              <Col md={12}><Form.Label>Description</Form.Label><Form.Control as="textarea" rows={2} value={productDraft.description} onChange={(event) => setProductDraft({ ...productDraft, description: event.target.value })} /></Col>
              <Col md={12}><Form.Check type="switch" label="Active" checked={productDraft.active} onChange={(event) => setProductDraft({ ...productDraft, active: event.target.checked })} /></Col>
            </Row>
            <div className="mt-3 d-flex gap-2">
              <Button type="submit" disabled={saving || (!isAdmin && !vendorId)}>{saving ? "Saving..." : editingProductId == null ? "Add product" : "Save product"}</Button>
              {editingProductId != null && <Button type="button" variant="outline-secondary" onClick={resetProductForm}>Cancel</Button>}
            </div>
          </Form>
        </div>
      </div>
    </section>
  );
}

export default CatalogManagementPanel;
